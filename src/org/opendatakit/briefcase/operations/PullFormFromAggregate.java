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
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.util.RetrieveAvailableFormsFromServer;
import org.opendatakit.briefcase.util.TransferFromServer;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFormFromAggregate {
  private static final Logger log = LoggerFactory.getLogger(PullFormFromAggregate.class);
  public static final Param<Void> DEPRECATED_PULL_AGGREGATE = Param.flag("pa", "Pull form from an Aggregate instance");
  private static final Param<Void> PULL_AGGREGATE = Param.flag("plla", "pull_aggregate", "Pull form from an Aggregate instance");

  public static Operation PULL_FORM_FROM_AGGREGATE = Operation.of(
      PULL_AGGREGATE,
      args -> pullFormFromAggregate(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(ODK_USERNAME),
          args.get(ODK_PASSWORD),
          args.get(AGGREGATE_SERVER)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER)
  );

  public static void pullFormFromAggregate(String storageDir, String formid, String username, String password, String baseUrl) {
    CliEventsCompanion.attach(log);
    bootCache(storageDir);
    ServerConnectionInfo transferSettings = new ServerConnectionInfo(baseUrl, username, password.toCharArray());

    RemoteServer server;
    try {
      server = RemoteServer.authenticated(new URL(baseUrl), new Credentials(username, password));
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }

    // TODO Drill down known errors like HTTP 404 or HTTP 401 and give richer feedback
    if (!server.testPull(new CommonsHttp()).map(__ -> true).orElse(false))
      throw new BriefcaseException("Can't connect to Aggregate");

    Optional<FormStatus> maybeForm = RetrieveAvailableFormsFromServer.get(transferSettings).stream()
        .filter(f -> f.getFormDefinition().getFormId().equals(formid))
        .findFirst();

    if (!maybeForm.isPresent())
      throw new FormNotFoundException(formid);

    FormStatus form = maybeForm.get();
    EventBus.publish(new StartPullEvent(form));
    TransferFromServer.pull(transferSettings, form);
  }

}
