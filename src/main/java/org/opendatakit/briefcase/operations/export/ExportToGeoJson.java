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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.operations.export.ExportOutcome.SOME_SKIPPED;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataQueries.sortedListOfSubmissionFiles;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.bushe.swing.event.EventBus;
import org.geojson.Feature;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.submission.Submission;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToGeoJson {
  private static final Logger log = LoggerFactory.getLogger(ExportToGeoJson.class);

  public static ExportOutcome export(SubmissionMetadataPort submissionMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration) {
    return export(submissionMetadataPort, formMetadata, formDef, configuration, Optional.empty());
  }

  public static ExportOutcome export(SubmissionMetadataPort submissionMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration, Analytics analytics) {
    return export(submissionMetadataPort, formMetadata, formDef, configuration, Optional.of(analytics));
  }

  /**
   * Export a form's submissions into a GeoJSON file.
   *
   * @param formDef       the {@link FormDefinition} form definition of the form to be exported
   * @param configuration the {@link ExportConfiguration} export configuration
   * @return an {@link ExportOutcome} with the export operation's outcome
   * @see ExportConfiguration
   */
  private static ExportOutcome export(SubmissionMetadataPort submissionMetadataPort, FormMetadata formMetadata, FormDefinition formDef, ExportConfiguration configuration, Optional<Analytics> analytics) {
    // Create an export tracker object with the total number of submissions we have to export
    ExportProcessTracker exportTracker = new ExportProcessTracker(formMetadata.getKey());
    exportTracker.start();

    var onParsingError = buildParsingErrorCallback(configuration.getErrorsDir(formDef.getFormName()));
    var onInvalidSubmission = buildParsingErrorCallback(configuration.getErrorsDir(formDef.getFormName()))
        .andThen((path, message) ->
            analytics.ifPresent(ga -> ga.event("Export", "Export", "invalid submission", null))
        );

    List<Path> submissionFiles = submissionMetadataPort
        .query(sortedListOfSubmissionFiles(formMetadata, configuration.getDateRange(), configuration.resolveSmartAppend()))
        .collect(toList());
    exportTracker.trackTotal(submissionFiles.size());

    createDirectories(configuration.getExportDir());

    // Generate csv lines grouped by the fqdn of the model they belong to
    Stream<Submission> validSubmissions = ExportTools.getValidSubmissions(formMetadata, formDef, configuration, submissionFiles, onParsingError, onInvalidSubmission);

    Stream<Feature> features = validSubmissions.peek(s -> exportTracker.incAndReport())
        .flatMap(submission -> GeoJson.toFeatures(formDef.getModel(), submission));

    Path output = configuration.getExportDir()
        .resolve(configuration.getFilenameBase(formDef.getFormName()) + ".geojson");
    GeoJson.write(output, features);

    exportTracker.end();

    ExportOutcome exportOutcome = exportTracker.computeOutcome();
    if (exportOutcome == ALL_EXPORTED)
      EventBus.publish(ExportEvent.successForm((int) exportTracker.total, formMetadata.getKey()));

    if (exportOutcome == SOME_SKIPPED)
      EventBus.publish(ExportEvent.partialSuccessForm((int) exportTracker.exported, (int) exportTracker.total, formMetadata.getKey()));

    if (exportOutcome == ALL_SKIPPED)
      EventBus.publish(ExportEvent.failure("All submissions have been skipped", formMetadata.getKey()));

    return exportOutcome;
  }

  private static BiConsumer<Path, String> buildParsingErrorCallback(Path errorsDir) {
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
