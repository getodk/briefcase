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

import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE;
import static org.opendatakit.briefcase.delivery.cli.Common.PULL_SOURCE_TYPE;
import static org.opendatakit.briefcase.delivery.cli.Common.getFormsToPull;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.COLLECT_DIRECTORY;
import static org.opendatakit.briefcase.operations.transfer.pull.Pull.buildPullJob;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromCollect {
  private static final Logger log = LoggerFactory.getLogger(PullFromCollect.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Pull from Collect")
        .withMatcher(args -> args.has(PULL) && args.has(PULL_SOURCE_TYPE, COLLECT_DIRECTORY))
        .withRequiredParams(PULL_SOURCE, PULL_SOURCE_TYPE)
        .withOptionalParams(FORM_ID)
        .withLauncher(args -> pull(container, args))
        .build();
  }

  private static void pull(Container container, Args args) {
    List<FormMetadata> formsToPull = getFormsToPull(args.getOptional(FORM_ID), Paths.get(args.get(PULL_SOURCE)));

    JobsRunner.launchAsync(formsToPull.stream()
        .map(formMetadata -> buildPullJob(
            container.workspace,
            container.http,
            container.formMetadata,
            container.submissionMetadata,
            formMetadata,
            PullFromCollect::onEvent,
            Optional.empty()
        )), PullFromCollect::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Pull complete");
    System.out.println();
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
