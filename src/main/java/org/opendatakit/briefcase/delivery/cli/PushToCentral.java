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

import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_EMAIL;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.PROJECT_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH_TARGET;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH_TARGET_TYPE;
import static org.opendatakit.briefcase.delivery.cli.Common.getFormsToPush;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.CENTRAL;
import static org.opendatakit.briefcase.operations.transfer.push.Push.buildPushJob;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.util.List;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToCentral {
  private static final Logger log = LoggerFactory.getLogger(PushToCentral.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Push to Central")
        .withMatcher(args -> args.has(PUSH) && args.has(PUSH_TARGET_TYPE, CENTRAL))
        .withRequiredParams(PUSH, PUSH_TARGET, PUSH_TARGET_TYPE, PROJECT_ID, CREDENTIALS_EMAIL, CREDENTIALS_PASSWORD)
        .withOptionalParams(FORM_ID)
        .withLauncher(args -> pushToCentral(container, args))
        .build();
  }

  private static void pushToCentral(Container container, Args args) {
    List<FormMetadata> formsToPush = getFormsToPush(container.formMetadata, args.getOptional(FORM_ID));

    JobsRunner.launchAsync(formsToPush.stream()
        .map(formMetadata -> buildPushJob(
            container.http,
            container.submissionMetadata,
            formMetadata,
            parseTarget(args),
            false,
            PushToCentral::onEvent
        )), PushToCentral::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Push complete");
    System.out.println();
  }

  private static String authenticate(Http http, CentralServer server) {
    Response<String> tokenResponse = http.execute(server.getSessionTokenRequest());
    if (!tokenResponse.isSuccess())
      throw new BriefcaseException(
          tokenResponse.isRedirection()
              ? "Error connecting to Central: Redirection detected"
              : tokenResponse.isUnauthorized()
              ? "Error connecting to Central: Wrong credentials"
              : tokenResponse.isNotFound()
              ? "Error connecting to Central: Central not found"
              : "Error connecting to Central"
      );
    return tokenResponse.get();
  }

  private static CentralServer parseTarget(Args args) {
    return CentralServer.of(
        url(args.get(PULL_SOURCE)),
        args.get(PROJECT_ID),
        new Credentials(args.get(CREDENTIALS_EMAIL), args.get(CREDENTIALS_PASSWORD))
    );
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
    // The PullTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pushing a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pushing a form", e);
  }

}
