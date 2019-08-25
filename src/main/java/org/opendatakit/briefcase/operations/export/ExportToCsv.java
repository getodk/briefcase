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

package org.opendatakit.briefcase.operations.export;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.reducing;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.SOME_SKIPPED;
import static org.opendatakit.briefcase.operations.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.write;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataCommands;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToCsv {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsv.class);

  public static ExportOutcome export(FormMetadataPort formMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration) {
    return export(formMetadataPort, formMetadata, formDef, configuration, Optional.empty());
  }

  public static ExportOutcome export(FormMetadataPort formMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration, Analytics analytics) {
    return export(formMetadataPort, formMetadata, formDef, configuration, Optional.of(analytics));
  }

  /**
   * Export a form's submissions into some CSV files.
   * <p>
   * If the form has repeat groups, each repeat group will be exported into a separate CSV file.
   *
   * @param formDef       the {@link FormDefinition} form definition of the form to be exported
   * @param configuration the {@link ExportConfiguration} export configuration
   * @return an {@link ExportOutcome} with the export operation's outcome
   * @see ExportConfiguration
   */
  private static ExportOutcome export(FormMetadataPort formMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration, Optional<Analytics> analytics) {
    // Create an export tracker object with the total number of submissions we have to export
    ExportProcessTracker exportTracker = new ExportProcessTracker(formMetadata.getKey());
    exportTracker.start();

    SubmissionExportErrorCallback onParsingError = buildParsingErrorCallback(configuration.getErrorsDir(formMetadata.getKey().getName()));
    SubmissionExportErrorCallback onInvalidSubmission = buildParsingErrorCallback(configuration.getErrorsDir(formMetadata.getKey().getName()))
        .andThen((path, message) ->
            analytics.ifPresent(ga -> ga.event("Export", "Export", "invalid submission", null))
        );

    List<Path> submissionFiles = getListOfSubmissionFiles(formMetadata, configuration.getDateRange(), configuration.resolveSmartAppend(), onParsingError);
    exportTracker.trackTotal(submissionFiles.size());

    createDirectories(configuration.getExportDir());

    List<Csv> csvs = Csv.getCsvs(formDef, configuration);

    csvs.forEach(Csv::prepareOutputFiles);

    if (formDef.getModel().hasAuditField()) {
      Path audit = configuration.getAuditPath(formMetadata.getKey().getName());
      if (!exists(audit) || configuration.resolveOverwriteExistingFiles())
        write(audit, "instance ID, event, node, start, end\n", CREATE, WRITE, TRUNCATE_EXISTING);
    }

    // Generate csv lines grouped by the fqdn of the model they belong to
    Map<String, CsvLines> csvLinesPerModel = ExportTools.getValidSubmissions(formMetadata, formDef, configuration, submissionFiles, onParsingError, onInvalidSubmission)
        // Track the submission
        .peek(s -> exportTracker.incAndReport())
        // Use the mapper of each Csv instance to map the submission into their respective outputs
        .flatMap(submission -> csvs.stream()
            .map(Csv::getMapper)
            .map(mapper -> mapper.apply(submission)))
        // Group and merge the CsvLines by the model they belong to
        .collect(groupingByConcurrent(
            CsvLines::getModelFqn,
            reducing(CsvLines.empty(), CsvLines::merge)
        ));

    // TODO We should have an extra step to produce the side effect of writing media files to disk to avoid having side-effects while generating the CSV output of binary fields

    // Write lines to each output Csv
    csvs.forEach(csv -> csv.appendLines(
        Optional.ofNullable(csvLinesPerModel.get(csv.getModelFqn())).orElse(CsvLines.empty())
    ));

    exportTracker.end();

    Optional.ofNullable(csvLinesPerModel.get(formDef.getModel().fqn()))
        .orElse(CsvLines.empty())
        .getLastLine()
        .map(line -> formMetadata.withLastExportedSubmissionDate(line.getSubmissionDate()))
        .map(FormMetadataCommands::upsert)
        .ifPresent(formMetadataPort::execute);

    ExportOutcome exportOutcome = exportTracker.computeOutcome();
    if (exportOutcome == ALL_EXPORTED)
      EventBus.publish(ExportEvent.successForm((int) exportTracker.total, formMetadata.getKey()));

    if (exportOutcome == SOME_SKIPPED)
      EventBus.publish(ExportEvent.partialSuccessForm((int) exportTracker.exported, (int) exportTracker.total, formMetadata.getKey()));

    if (exportOutcome == ALL_SKIPPED)
      EventBus.publish(ExportEvent.failure("All submissions have been skipped", formMetadata.getKey()));

    return exportOutcome;
  }

  private static SubmissionExportErrorCallback buildParsingErrorCallback(Path errorsDir) {
    AtomicInteger errorSeq = new AtomicInteger(1);
    // Remove errors from a previous export attempt
    if (exists(errorsDir))
      deleteRecursive(errorsDir);
    return (path, message) -> {
      if (!exists(errorsDir))
        createDirectories(errorsDir);
      copy(path, errorsDir.resolve("failed_submission_" + errorSeq.getAndIncrement() + ".xml"));
      log.warn("A submission has been excluded from the export output due to some problem ({}). If you didn't expect this, please ask for support at https://forum.opendatakit.org/c/support", message);
    };
  }

}
