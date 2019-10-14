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
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.INCLUDE_INCOMPLETE;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE_TYPE;
import static org.opendatakit.briefcase.delivery.cli.Common.START_FROM_DATE;
import static org.opendatakit.briefcase.delivery.cli.Common.START_FROM_LAST;
import static org.opendatakit.briefcase.delivery.cli.Common.getFormsToPull;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.AGGREGATE;
import static org.opendatakit.briefcase.operations.transfer.pull.Pull.buildPullJob;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataQueries.lastCursorOf;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.api.Optionals;
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

public class PullFromAggregate {
  private static final Logger log = LoggerFactory.getLogger(PullFromAggregate.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Pull from Aggregate")
        .withMatcher(args -> args.has(PULL) && args.has(PULL_SOURCE_TYPE, AGGREGATE))
        .withRequiredParams(PULL, PULL_SOURCE, PULL_SOURCE_TYPE)
        .withOptionalParams(FORM_ID, CREDENTIALS_USERNAME, CREDENTIALS_PASSWORD, START_FROM_LAST, START_FROM_DATE, INCLUDE_INCOMPLETE)
        .withLauncher(args -> pullFormFromAggregate(container, args))
        .build();
  }

  private static void pullFormFromAggregate(Container container, Args args) {
    List<FormMetadata> formsToPull = getFormsToPull(container.http, args.getOptional(FORM_ID), parseSource(args).getFormListRequest());

    JobsRunner.launchAsync(formsToPull.stream()
        .map(formMetadata -> buildPullJob(
            container.workspace,
            container.http,
            container.formMetadata,
            container.submissionMetadata,
            formMetadata,
            PullFromAggregate::onEvent,
            resolveCursor(
                args.getOptional(START_FROM_DATE).map(OffsetDateTime::toLocalDate),
                args.has(START_FROM_LAST),
                container.formMetadata.query(lastCursorOf(formMetadata.getKey()))
            )
        )), PullFromAggregate::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Pull complete");
    System.out.println();
  }

  private static AggregateServer parseSource(Args args) {
    Optional<Credentials> maybeCredentials = OptionalProduct.all(
        args.getOptional(CREDENTIALS_USERNAME),
        args.getOptional(CREDENTIALS_PASSWORD)
    ).map(Credentials::from);

    return maybeCredentials
        .map(credentials -> AggregateServer.authenticated(url(args.get(PULL_SOURCE)), credentials))
        .orElseGet(() -> AggregateServer.normal(url(args.get(PULL_SOURCE))));
  }

  private static Optional<Cursor> resolveCursor(Optional<LocalDate> startFromDate, boolean startFromLast, Optional<Cursor> storedCursor) {
    return Optionals.race(
        startFromDate.map(Cursor::of),
        startFromLast ? storedCursor : Optional.empty()
    );
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
    // The PullFromAggregateTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pulling a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pulling a form", e);
  }

}
