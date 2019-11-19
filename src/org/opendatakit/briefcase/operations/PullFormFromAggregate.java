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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.form.FormMetadataQueries.lastCursorOf;
import static org.opendatakit.briefcase.operations.Common.CREDENTIALS_PASSWORD;
import static org.opendatakit.briefcase.operations.Common.CREDENTIALS_USERNAME;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.operations.Common.SERVER_URL;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.form.FileSystemFormMetadataAdapter;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Optionals;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFormFromAggregate {
  private static final Logger log = LoggerFactory.getLogger(PullFormFromAggregate.class);
  public static final Param<Void> DEPRECATED_PULL_AGGREGATE = Param.flag("pa", "pull_aggregate", "(Deprecated. Use -plla instead)");
  public static final Param<Void> PULL_AGGREGATE = Param.flag("plla", "pull_aggregate", "Pull form from an Aggregate instance");
  public static final Param<Void> DEPRECATED_PULL_IN_PARALLEL = Param.flag("pp", "parallel_pull", "(Deprecated. Use -mhc instead)");
  private static final Param<Void> RESUME_LAST_PULL = Param.flag("sfl", "start_from_last", "Start pull from last submission pulled");
  private static final Param<LocalDate> START_FROM_DATE = Param.arg("sfd", "start_from_date", "Start pull from date", LocalDate::parse);
  private static final Param<Void> INCLUDE_INCOMPLETE = Param.flag("ii", "include_incomplete", "Include incomplete submissions");

  public static Operation PULL_FORM_FROM_AGGREGATE = Operation.of(
      PULL_AGGREGATE,
      args -> pullFormFromAggregate(
          args.get(STORAGE_DIR),
          args.getOptional(FORM_ID),
          args.get(CREDENTIALS_USERNAME),
          args.get(CREDENTIALS_PASSWORD),
          args.get(SERVER_URL),
          args.has(RESUME_LAST_PULL),
          args.getOptional(START_FROM_DATE),
          args.has(INCLUDE_INCOMPLETE),
          args.getOptional(MAX_HTTP_CONNECTIONS)
      ),
      Arrays.asList(STORAGE_DIR, CREDENTIALS_USERNAME, CREDENTIALS_PASSWORD, SERVER_URL),
      Arrays.asList(RESUME_LAST_PULL, INCLUDE_INCOMPLETE, FORM_ID, START_FROM_DATE, MAX_HTTP_CONNECTIONS)
  );

  public static void pullFormFromAggregate(Path storageDir, Optional<String> formId, String username, String password, URL server, boolean resumeLastPull, Optional<LocalDate> startFromDate, boolean includeIncomplete, Optional<Integer> maybeMaxHttpConnections) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    FormMetadataPort formMetadataPort = FileSystemFormMetadataAdapter.at(briefcaseDir);

    int maxHttpConnections = Optionals.race(
        maybeMaxHttpConnections,
        appPreferences.getMaxHttpConnections()
    ).orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    AggregateServer aggregateServer = AggregateServer.authenticated(server, new Credentials(username, password));

    Response<List<RemoteFormDefinition>> response = http.execute(aggregateServer.getFormListRequest());
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

    List<FormStatus> filteredForms = response.orElseThrow(BriefcaseException::new)
        .stream()
        .filter(f -> formId.map(id -> f.getFormId().equals(id)).orElse(true))
        .map(FormStatus::new)
        .collect(toList());

    if (formId.isPresent() && filteredForms.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    TransferForms forms = TransferForms.empty();
    forms.load(filteredForms);
    forms.selectAll();

    PullFromAggregate pullOp = new PullFromAggregate(http, aggregateServer, briefcaseDir, includeIncomplete, PullFormFromAggregate::onEvent, formMetadataPort);
    JobsRunner.launchAsync(
        forms.map(form -> pullOp.pull(form, resolveCursor(
            resumeLastPull,
            startFromDate,
            form,
            formMetadataPort
        ))),
        PullFormFromAggregate::onError
    ).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
  }

  private static Optional<Cursor> resolveCursor(boolean resumeLastPull, Optional<LocalDate> startFromDate, FormStatus form, FormMetadataPort formMetadataPort) {
    FormKey key = FormKey.from(form);
    return Optionals.race(
        startFromDate.map(Cursor::of),
        resumeLastPull
            ? formMetadataPort.query(lastCursorOf(key))
            : Optional.empty()
    );
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getStatus().getFormName() + " - " + event.getStatusString());
    // The PullFromAggregateTracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pulling a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pulling a form", e);
  }

}
