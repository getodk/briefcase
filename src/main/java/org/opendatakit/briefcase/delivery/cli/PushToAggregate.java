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

import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_USERNAME;
import static org.opendatakit.briefcase.delivery.cli.Common.FORCE_SEND_BLANK;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH_TARGET;
import static org.opendatakit.briefcase.delivery.cli.Common.PUSH_TARGET_TYPE;
import static org.opendatakit.briefcase.delivery.cli.Common.getFormsToPush;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.AGGREGATE;
import static org.opendatakit.briefcase.operations.transfer.push.Push.buildPushJob;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToAggregate {
  private static final Logger log = LoggerFactory.getLogger(PushToAggregate.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Push to Aggregate")
        .withMatcher(args -> args.has(PUSH) && args.has(PUSH_TARGET_TYPE, AGGREGATE))
        .withRequiredParams(PUSH, PUSH_TARGET, PUSH_TARGET_TYPE)
        .withOptionalParams(FORM_ID, CREDENTIALS_USERNAME, CREDENTIALS_PASSWORD, FORCE_SEND_BLANK)
        .withLauncher(args -> pushFormToAggregate(container, args))
        .build();
  }

  private static void pushFormToAggregate(Container container, Args args) {
    List<FormMetadata> formsToPush = getFormsToPush(container.formMetadata, args.getOptional(FORM_ID));

    JobsRunner.launchAsync(formsToPush.stream()
        .map(formMetadata -> buildPushJob(
            container.http,
            container.submissionMetadata,
            formMetadata,
            parseTarget(args),
            args.has(FORCE_SEND_BLANK),
            PushToAggregate::onEvent
        )), PushToAggregate::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Push complete");
    System.out.println();
  }

  private static AggregateServer parseTarget(Args args) {
    Optional<Credentials> maybeCredentials = OptionalProduct.all(
        args.getOptional(CREDENTIALS_USERNAME),
        args.getOptional(CREDENTIALS_PASSWORD)
    ).map(Credentials::from);

    return maybeCredentials
        .map(credentials -> AggregateServer.authenticated(url(args.get(PULL_SOURCE)), credentials))
        .orElseGet(() -> AggregateServer.normal(url(args.get(PULL_SOURCE))));
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
