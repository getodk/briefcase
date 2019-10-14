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

import static org.opendatakit.briefcase.delivery.cli.Common.EXPORT;
import static org.opendatakit.briefcase.delivery.cli.Common.EXPORT_ATTACHMENTS;
import static org.opendatakit.briefcase.delivery.cli.Common.EXPORT_DIR;
import static org.opendatakit.briefcase.delivery.cli.Common.EXPORT_END_DATE;
import static org.opendatakit.briefcase.delivery.cli.Common.EXPORT_START_DATE;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.INCLUDE_GEOJSON_EXPORT;
import static org.opendatakit.briefcase.delivery.cli.Common.OVERWRITE;
import static org.opendatakit.briefcase.delivery.cli.Common.PEM_FILE;
import static org.opendatakit.briefcase.delivery.cli.Common.REMOVE_GROUP_NAMES;
import static org.opendatakit.briefcase.delivery.cli.Common.SMART_APPEND;
import static org.opendatakit.briefcase.delivery.cli.Common.SPLIT_SELECT_MULTIPLES;
import static org.opendatakit.briefcase.delivery.cli.Common.START_FROM_LAST;
import static org.opendatakit.briefcase.operations.transfer.pull.Pull.buildPullJob;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportToCsv;
import org.opendatakit.briefcase.operations.export.ExportToGeoJson;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Export {
  private static final Logger log = LoggerFactory.getLogger(Export.class);

  public static Operation create(Container container) {
    return OperationBuilder.cli("Export")
        .withMatcher(args -> args.has(EXPORT))
        .withRequiredParams(EXPORT, EXPORT_DIR, FORM_ID)
        .withOptionalParams(PEM_FILE, EXPORT_ATTACHMENTS, OVERWRITE, EXPORT_START_DATE, EXPORT_END_DATE, START_FROM_LAST, SPLIT_SELECT_MULTIPLES, INCLUDE_GEOJSON_EXPORT, REMOVE_GROUP_NAMES, SMART_APPEND)
        .withLauncher(args -> export(container, args))
        .build();
  }

  private static void export(Container container, Args args) {
    CliEventsCompanion.attach(log);

    DateRange dateRange = new DateRange(
        args.getOptional(EXPORT_START_DATE).map(OffsetDateTime::toLocalDate),
        args.getOptional(EXPORT_END_DATE).map(OffsetDateTime::toLocalDate)
    );

    FormMetadata formMetadata = Common.getFormsToPush(container.formMetadata, args.getOptional(FORM_ID)).get(0);

    ExportConfiguration configuration = ExportConfiguration.Builder.empty()
        .setExportDir(args.get(EXPORT_DIR))
        .setPemFile(args.getOptional(PEM_FILE))
        .setDateRange(dateRange)
        .setStartFromLast(args.has(START_FROM_LAST))
        .setOverwriteFiles(args.has(OVERWRITE))
        .setExportAttachments(args.has(EXPORT_ATTACHMENTS))
        .setSplitSelectMultiples(args.has(SPLIT_SELECT_MULTIPLES))
        .setIncludeGeoJsonExport(args.has(INCLUDE_GEOJSON_EXPORT))
        .setRemoveGroupNames(args.has(REMOVE_GROUP_NAMES))
        .setSmartAppend(args.has(SMART_APPEND))
        .build();

    Job<Void> pullJob = configuration.resolvePullBefore() && formMetadata.getPullSource().isPresent() ?
        buildPullJob(
            container.workspace,
            container.http,
            container.formMetadata,
            container.submissionMetadata,
            formMetadata,
            Export::onEvent,
            args.has(START_FROM_LAST) ? Optional.of(formMetadata.getCursor()) : Optional.empty()
        ) : Job.noOpSupplier();

    FormDefinition formDef = FormDefinition.from(formMetadata);

    Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(formMetadata, formDef, configuration, container.submissionMetadata, container.formMetadata));

    Job<Void> exportGeoJsonJob = configuration.resolveIncludeGeoJsonExport()
        ? Job.run(runnerStatus -> ExportToGeoJson.export(container, formMetadata, formDef, configuration))
        : Job.noOp;

    JobsRunner.launchAsync(
        pullJob.thenRun(exportJob).thenRun(exportGeoJsonJob),
        Export::onError
    ).waitForCompletion();

    System.out.println();
    System.out.println("Export complete");
    System.out.println();
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
  }

  private static void onError(Throwable e) {
    System.err.println("Error exporting a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error exporting a form", e);
  }
}
