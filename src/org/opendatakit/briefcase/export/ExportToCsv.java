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

package org.opendatakit.briefcase.export;

import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.export.ExportOutcome.SOME_SKIPPED;
import static org.opendatakit.briefcase.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.export.SubmissionParser.parseSubmission;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToCsv {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsv.class);

  /**
   * Export a form's submissions into some CSV files.
   * <p>
   * If the form has repeat groups, each repeat group will be exported into a separate CSV file.
   *
   * @param formDefinition the {@link BriefcaseFormDefinition} form definition of the form to be exported
   * @param configuration  the {@link ExportConfiguration} export configuration
   * @param exportMedia    a {@link Boolean} indicating if media files attached to each submission must be also exported
   * @return an {@link ExportOutcome} with the export operation's outcome
   * @see ExportConfiguration
   */
  public static ExportOutcome export(FormDefinition formDefinition, ExportConfiguration configuration, boolean exportMedia) {
    // Create an export tracker object with the total number of submissions we have to export
    ExportProcessTracker exportTracker = new ExportProcessTracker(formDefinition);
    exportTracker.start();
    log.debug("Start export");
    // List all submission files we want to export
    List<Path> submissionFiles = getListOfSubmissionFiles(formDefinition, configuration.getDateRange());
    log.debug("Files listed");
    exportTracker.trackTotal(submissionFiles.size());

    // Compute and create the export directory
    createDirectories(configuration.getExportDir().orElseThrow(() -> new BriefcaseException("No export dir defined")));

    // Prepare the list of csv files we will export
    List<Csv> csvs = new ArrayList<>();
    csvs.add(Csv.main(formDefinition, configuration, exportMedia));
    csvs.addAll(formDefinition.getModel().getRepeatableFields().stream()
        .map(groupModel -> Csv.repeat(formDefinition, groupModel, configuration, exportMedia))
        .collect(toList()));

    // Prepare the output file
    csvs.forEach(Csv::prepareOutput);

    ConcurrentMap<String, CsvLines> collect = submissionFiles.parallelStream()
        // Parse the submission and leave only those OK to be exported
        .map(path -> parseSubmission(path, formDefinition.isFileEncryptedForm(), configuration.getPrivateKey()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        // Track the submission
        .peek(s -> exportTracker.incAndReport())
        // Transform each submission into a list of CsvLines (one per output Csv)
        .flatMap(submission -> csvs.stream()
            .map(Csv::getMapper)
            .map(mapper -> mapper.apply(submission)))
        // Group and merge the CsvLines by the model they belong to
        .collect(groupingByConcurrent(
            CsvLines::getModelFqn,
            reducing(CsvLines.empty(), CsvLines::merge)
        ));

    log.debug("Submissions transformed to csv lines");

    // Write lines to each output Csv
    csvs.forEach(csv -> csv.appendLines(
        Optional.ofNullable(collect.get(csv.getModelFqn())).orElse(CsvLines.empty())
    ));

    log.debug("Files written");

    // Mark the end of the export and report
    exportTracker.end();

    ExportOutcome exportOutcome = exportTracker.computeOutcome();
    if (exportOutcome == ALL_EXPORTED)
      EventBus.publish(ExportEvent.successForm(formDefinition, (int) exportTracker.total));

    if (exportOutcome == SOME_SKIPPED)
      EventBus.publish(ExportEvent.partialSuccessForm(formDefinition, (int) exportTracker.exported, (int) exportTracker.total));

    if (exportOutcome == ALL_SKIPPED)
      EventBus.publish(ExportEvent.failure(formDefinition, "All submissions have been skipped"));

    return exportOutcome;
  }

}
