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
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.delivery.cli.Common.CREDENTIALS_USERNAME;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.delivery.cli.Common.SERVER_URL;
import static org.opendatakit.briefcase.delivery.cli.Common.WORKSPACE_LOCATION;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToAggregate {
  private static final Logger log = LoggerFactory.getLogger(PushToAggregate.class);
  private static final Param<Void> PUSH_AGGREGATE = Param.flag("psha", "push_aggregate", "Push form to an Aggregate instance");
  private static final Param<Void> FORCE_SEND_BLANK = Param.flag("fsb", "force_send_blank", "Force sending the blank form to the Aggregate instance");

  public static Operation create(FormMetadataPort formMetadataPort) {
    return new OperationBuilder()
        .withFlag(PUSH_AGGREGATE)
        .withRequiredParams(WORKSPACE_LOCATION, CREDENTIALS_USERNAME, CREDENTIALS_PASSWORD, SERVER_URL)
        .withOptionalParams(FORCE_SEND_BLANK, MAX_HTTP_CONNECTIONS, FORM_ID)
        .withLauncher(args -> pushFormToAggregate(formMetadataPort, args))
        .build();
  }

  private static void pushFormToAggregate(FormMetadataPort formMetadataPort, Args args) {
    Optional<String> formid = args.getOptional(FORM_ID);
    String username = args.get(CREDENTIALS_USERNAME);
    String password = args.get(CREDENTIALS_PASSWORD);
    URL server = args.get(SERVER_URL);
    boolean forceSendBlank = args.has(FORCE_SEND_BLANK);
    Optional<Integer> maybeMaxConnections = args.getOptional(MAX_HTTP_CONNECTIONS);

    CliEventsCompanion.attach(log);
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();

    int maxHttpConnections = Optionals.race(
        maybeMaxConnections,
        appPreferences.getMaxHttpConnections()
    ).orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    AggregateServer aggregateServer = AggregateServer.authenticated(server, new Credentials(username, password));

    Response response = http.execute(aggregateServer.getPushFormPreflightRequest());
    if (!response.isSuccess()) {
      System.err.println(response.isRedirection()
          ? "Error connecting to Aggregate: Redirection detected"
          : response.isUnauthorized()
          ? "Error connecting to Aggregate: Wrong credentials"
          : response.isNotFound()
          ? "Error connecting to Aggregate: Aggregate not found"
          : "Error connecting to Aggregate");
      return;
    }

    List<FormMetadata> formMetadataList;
    if (formid.isPresent()) {
      String requestedFormId = formid.get();
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

    org.opendatakit.briefcase.operations.transfer.push.aggregate.PushToAggregate pushOp = new org.opendatakit.briefcase.operations.transfer.push.aggregate.PushToAggregate(http, aggregateServer, forceSendBlank, PushToAggregate::onEvent);
    JobsRunner.launchAsync(forms.map(pushOp::push), PushToAggregate::onError).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
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
