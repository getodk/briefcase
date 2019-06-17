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
import static org.opendatakit.briefcase.operations.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.operations.Common.ODK_PASSWORD;
import static org.opendatakit.briefcase.operations.Common.ODK_USERNAME;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.push.aggregate.PushToAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Optionals;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.FormCache;
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
          args.has(FORCE_SEND_BLANK),
          args.getOptional(MAX_HTTP_CONNECTIONS)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER),
      Arrays.asList(FORCE_SEND_BLANK, MAX_HTTP_CONNECTIONS)
  );

  private static void pushFormToAggregate(String storageDir, String formid, String username, String password, String server, boolean forceSendBlank, Optional<Integer> maybeMaxConnections) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();

    int maxConnections = Optionals.race(
        maybeMaxConnections,
        appPreferences.getMaxHttpConnections()
    ).orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxConnections));

    AggregateServer aggregateServer = AggregateServer.authenticated(RequestBuilder.url(server), new Credentials(username, password));

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
    Optional<FormStatus> maybeFormStatus = formCache.getForms().stream()
        .filter(form -> form.getFormId().equals(formid))
        .map(FormStatus::new)
        .findFirst();

    FormStatus form = maybeFormStatus.orElseThrow(() -> new BriefcaseException("Form " + formid + " not found"));
    TransferForms forms = TransferForms.of(form);
    forms.selectAll();

    PushToAggregate pushOp = new PushToAggregate(http, aggregateServer, briefcaseDir, forceSendBlank, PushFormToAggregate::onEvent);
    JobsRunner.launchAsync(
        forms.map(pushOp::push),
        __ -> { },
        PushFormToAggregate::onError
    ).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getStatus().getFormName() + " - " + event.getStatusString());
    // The PullTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pushing a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pushing a form", e);
  }

}
