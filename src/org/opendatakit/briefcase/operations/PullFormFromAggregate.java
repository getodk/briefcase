/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.AGGREGATE_SERVER;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.ODK_PASSWORD;
import static org.opendatakit.briefcase.operations.Common.ODK_USERNAME;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Response;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.briefcase.util.RetrieveAvailableFormsFromServer;
import org.opendatakit.briefcase.util.TransferFromServer;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFormFromAggregate {
  private static final Logger log = LoggerFactory.getLogger(PullFormFromAggregate.class);
  public static final Param<Void> DEPRECATED_PULL_AGGREGATE = Param.flag("pa", "pull_aggregate", "(Deprecated)");
  private static final Param<Void> PULL_AGGREGATE = Param.flag("plla", "pull_aggregate", "Pull form from an Aggregate instance");
  private static final Param<Void> PULL_IN_PARALLEL = Param.flag("pp", "parallel_pull", "Pull submissions in parallel");
  private static final Param<Void> RESUME_LAST_PULL = Param.flag("sfl", "start_from_last", "Start pull from last submission pulled");
  private static final Param<LocalDate> START_FROM_DATE = Param.arg("sfd", "start_from_date", "Start pull from date", LocalDate::parse);
  private static final Param<Void> INCLUDE_INCOMPLETE = Param.flag("ii", "include_incomplete", "Include incomplete submissions");

  public static Operation PULL_FORM_FROM_AGGREGATE = Operation.of(
      PULL_AGGREGATE,
      args -> pullFormFromAggregate(
          args.get(STORAGE_DIR),
          args.getOptional(FORM_ID),
          args.get(ODK_USERNAME),
          args.get(ODK_PASSWORD),
          args.get(AGGREGATE_SERVER),
          args.has(PULL_IN_PARALLEL),
          args.has(RESUME_LAST_PULL),
          args.getOptional(START_FROM_DATE),
          args.has(INCLUDE_INCOMPLETE)
      ),
      Arrays.asList(STORAGE_DIR, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER),
      Arrays.asList(PULL_IN_PARALLEL, RESUME_LAST_PULL, INCLUDE_INCOMPLETE, FORM_ID, START_FROM_DATE)
  );

  public static void pullFormFromAggregate(String storageDir, Optional<String> formId, String username, String password, String server, boolean pullInParallel, boolean resumeLastPull, Optional<LocalDate> startFromDate, boolean includeIncomplete) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();

    CommonsHttp http = new CommonsHttp();

    URL baseUrl;
    try {
      baseUrl = new URL(server);
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }
    RemoteServer remoteServer = RemoteServer.authenticated(baseUrl, new Credentials(username, password));

    Response<Boolean> response = remoteServer.testPull(http);
    if (!response.isSuccess())
      System.err.println(response.isRedirection()
          ? "Error connecting to Aggregate: Redirection detected"
          : response.isUnauthorized()
          ? "Error connecting to Aggregate: Wrong credentials"
          : response.isNotFound()
          ? "Error connecting to Aggregate: Aggregate not found"
          : "Error connecting to Aggregate");
    else {
      TransferForms forms = TransferForms.from(RetrieveAvailableFormsFromServer.get(remoteServer.asServerConnectionInfo()).stream()
          .filter(f -> formId.map(id -> f.getFormDefinition().getFormId().equals(id)).orElse(true))
          .collect(Collectors.toList()));

      if (formId.isPresent() && forms.isEmpty())
        throw new FormNotFoundException(formId.get());

      forms.selectAll();

      TransferFromServer.pull(remoteServer.asServerConnectionInfo(), briefcaseDir, pullInParallel, includeIncomplete, forms, resumeLastPull, startFromDate);
    }
  }

}
