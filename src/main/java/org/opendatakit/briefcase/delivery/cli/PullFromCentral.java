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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_EMAIL;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.delivery.cli.Common.PROJECT_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.SERVER_URL;
import static org.opendatakit.briefcase.delivery.cli.Common.WORKSPACE_LOCATION;

import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromCentral {
  private static final Logger log = LoggerFactory.getLogger(PullFromCentral.class);
  private static final Param<Void> PULL_FROM_CENTRAL = Param.flag("pllc", "pull_central", "Pull form from a Central server");

  public static Operation create(Container container) {
    return new OperationBuilder()
        .withFlag(PULL_FROM_CENTRAL)
        .withRequiredParams(WORKSPACE_LOCATION, SERVER_URL, PROJECT_ID, CREDENTIALS_EMAIL, CREDENTIALS_PASSWORD)
        .withOptionalParams(FORM_ID, MAX_HTTP_CONNECTIONS)
        .withLauncher(args -> pullFromCentral(container, args))
        .build();
  }

  private static void pullFromCentral(Container container, Args args) {
    CliEventsCompanion.attach(log);

    CentralServer server = CentralServer.of(args.get(SERVER_URL), args.get(PROJECT_ID), new Credentials(args.get(CREDENTIALS_EMAIL), args.get(CREDENTIALS_PASSWORD)));

    String token = container.http.execute(server.getSessionTokenRequest())
        .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));

    Response<List<FormMetadata>> response = container.http.execute(server.getFormsListRequest(token));
    if (!response.isSuccess()) {
      System.err.println(response.isRedirection()
          ? "Error connecting to Central: Redirection detected"
          : response.isUnauthorized()
          ? "Error connecting to Central: Wrong credentials"
          : response.isNotFound()
          ? "Error connecting to Central: Central not found"
          : "Error connecting to Central");
      return;
    }

    Optional<String> formId = args.getOptional(FORM_ID);

    List<FormMetadata> filteredForms = response.orElseThrow(BriefcaseException::new)
        .stream()
        .filter(f -> formId.map(id -> f.getKey().getId().equals(id)).orElse(true))
        .map(formMetadata -> formMetadata.withFormFile(container.workspace.buildFormFile(formMetadata)))
        .collect(toList());

    if (formId.isPresent() && filteredForms.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    TransferForms forms = TransferForms.empty();
    forms.load(filteredForms);
    forms.selectAll();

    org.opendatakit.briefcase.operations.transfer.pull.central.PullFromCentral pullOp = new org.opendatakit.briefcase.operations.transfer.pull.central.PullFromCentral(container, server, token, PullFromCentral::onEvent);
    JobsRunner.launchAsync(
        forms.map(formMetadata -> pullOp.pull(formMetadata, container.workspace.buildFormFile(formMetadata))),
        PullFromCentral::onError
    ).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
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
