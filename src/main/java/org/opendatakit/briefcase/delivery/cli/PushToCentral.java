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
package org.opendatakit.briefcase.delivery.cli;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_EMAIL;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.delivery.cli.Common.PROJECT_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.SERVER_URL;
import static org.opendatakit.briefcase.delivery.cli.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToCentral {
  private static final Logger log = LoggerFactory.getLogger(PushToCentral.class);
  private static final Param<Void> PUSH_TO_CENTRAL = Param.flag("pshc", "push_central", "Push form to a Central server");

  public static Operation create(FormMetadataPort formMetadataPort) {
    return Operation.of(
        PUSH_TO_CENTRAL,
        args -> pushToCentral(formMetadataPort, args),
        Arrays.asList(STORAGE_DIR, PROJECT_ID, CREDENTIALS_EMAIL, CREDENTIALS_PASSWORD, SERVER_URL),
        Arrays.asList(MAX_HTTP_CONNECTIONS, FORM_ID)
    );
  }

  private static void pushToCentral(FormMetadataPort formMetadataPort, Args args) {
    CliEventsCompanion.attach(log);
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();

    int maxHttpConnections = Optionals.race(
        args.getOptional(MAX_HTTP_CONNECTIONS),
        appPreferences.getMaxHttpConnections()
    ).orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    CentralServer server = CentralServer.of(args.get(SERVER_URL), args.get(PROJECT_ID), new Credentials(args.get(CREDENTIALS_EMAIL), args.get(CREDENTIALS_PASSWORD)));

    String token = http.execute(server.getSessionTokenRequest())
        .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));

    List<FormMetadata> formMetadataList;
    if (args.getOptional(FORM_ID).isPresent()) {
      String requestedFormId = args.getOptional(FORM_ID).get();
      Optional<FormMetadata> maybeFormStatus = formMetadataPort.fetchAll()
          .filter(formMetadata -> formMetadata.getKey().getId().equals(requestedFormId))
          .findFirst();
      FormMetadata formMetadata = maybeFormStatus.orElseThrow(() -> new BriefcaseException("Form " + requestedFormId + " not found"));
      formMetadataList = singletonList(formMetadata);
    } else {
      formMetadataList = formMetadataPort.fetchAll().collect(toList());
    }

    TransferForms forms = TransferForms.of(formMetadataList);
    forms.selectAll();

    org.opendatakit.briefcase.operations.transfer.push.central.PushToCentral pushOp = new org.opendatakit.briefcase.operations.transfer.push.central.PushToCentral(http, server, token, PushToCentral::onEvent);
    JobsRunner.launchAsync(forms.map(pushOp::push), PushToCentral::onError).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getName() + " - " + event.getMessage());
    // The PullTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pushing a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pushing a form", e);
  }

}
