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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Response;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.briefcase.util.TransferToServer;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushFormToAggregate {
  private static final Logger log = LoggerFactory.getLogger(PushFormToAggregate.class);
  private static final Param<Void> PUSH_AGGREGATE = Param.flag("psha", "push_aggregate", "Push form to an Aggregate instance");
  private static final Param<Void> FORCE_SEND_BLANK = Param.flag("fsb", "force_send_blank", "Force sending the blank form to the Aggregate instance");

  public static Operation PUSH_FORM_TO_AGGREGATE = Operation.of(
      PUSH_AGGREGATE,
      args -> pushFormToAggregate(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(ODK_USERNAME),
          args.get(ODK_PASSWORD),
          args.get(AGGREGATE_SERVER),
          args.has(FORCE_SEND_BLANK)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER),
      Collections.singletonList(FORCE_SEND_BLANK)
  );

  private static void pushFormToAggregate(String storageDir, String formid, String username, String password, String server, boolean forceSendBlank) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = BriefcasePreferences.buildBriefcaseDir(Paths.get(storageDir));
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

    Response<Boolean> response = remoteServer.testPush(http);
    if (!response.isSuccess())
      System.err.println(response.isRedirection()
          ? "Error connecting to Aggregate: Redirection detected"
          : response.isUnauthorized()
          ? "Error connecting to Aggregate: Wrong credentials"
          : response.isNotFound()
          ? "Error connecting to Aggregate: Aggregate not found"
          : "Error connecting to Aggregate");
    else {
      Optional<FormStatus> maybeFormStatus = formCache.getForms().stream()
          .filter(form -> form.getFormId().equals(formid))
          .map(formDef -> new FormStatus(FormStatus.TransferType.UPLOAD, formDef))
          .findFirst();

      FormStatus form = maybeFormStatus.orElseThrow(() -> new FormNotFoundException(formid));

      TransferToServer.push(remoteServer.asServerConnectionInfo(), http, remoteServer, forceSendBlank, form);
    }
  }

}
