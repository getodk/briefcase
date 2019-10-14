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
import static org.opendatakit.briefcase.delivery.cli.Common.PULL;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE_TYPE;
import static org.opendatakit.briefcase.delivery.cli.Common.getFormsToPull;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.CENTRAL;
import static org.opendatakit.briefcase.operations.transfer.pull.Pull.buildPullJob;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.util.List;
import java.util.Optional;
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

public class PullFromCentral {
  private static final Logger log = LoggerFactory.getLogger(PullFromCentral.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Pull from Central")
        .withMatcher(args -> args.has(PULL) && args.has(PULL_SOURCE_TYPE, CENTRAL))
        .withRequiredParams(PULL, PULL_SOURCE, PULL_SOURCE_TYPE, PROJECT_ID, CREDENTIALS_EMAIL, CREDENTIALS_PASSWORD)
        .withOptionalParams(FORM_ID)
        .withLauncher(args -> pullFromCentral(container, args))
        .build();
  }

  private static void pullFromCentral(Container container, Args args) {
    CentralServer source = parseSource(args);
    String token = authenticate(container.http, source);

    List<FormMetadata> formsToPull = getFormsToPull(container.http, args.getOptional(FORM_ID), source.getFormsListRequest(token));

    JobsRunner.launchAsync(formsToPull.stream()
        .map(formMetadata -> buildPullJob(
            container.workspace,
            container.http,
            container.formMetadata,
            container.submissionMetadata,
            formMetadata,
            PullFromCentral::onEvent,
            Optional.empty()
        )), PullFromCentral::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Pull complete");
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

  private static CentralServer parseSource(Args args) {
    return CentralServer.of(
        url(args.get(PULL_SOURCE)),
        args.get(PROJECT_ID),
        new Credentials(args.get(CREDENTIALS_EMAIL), args.get(CREDENTIALS_PASSWORD))
    );
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
    // The tracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pulling a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pulling a form", e);
  }

}
