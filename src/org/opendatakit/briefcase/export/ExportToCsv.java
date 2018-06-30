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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Comparator.comparing;
import static org.opendatakit.briefcase.export.CsvMapper.getMainHeader;
import static org.opendatakit.briefcase.export.CsvMapper.getMainSubmissionLines;
import static org.opendatakit.briefcase.export.CsvMapper.getRepeatCsvLine;
import static org.opendatakit.briefcase.export.CsvMapper.getRepeatHeader;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.export.ExportOutcome.SOME_SKIPPED;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToCsv {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsv.class);

  /**
   * Export a form's submissions into some CSV files.
   * <p>
   * If the form has repeat groups, each repeat group will be exported into a separate CSV file.
   *
   * @param formDef       the {@link BriefcaseFormDefinition} form definition of the form to be exported
   * @param configuration the {@link ExportConfiguration} export configuration
   * @param exportMedia   a {@link Boolean} indicating if media files attached to each submission must be also exported
   * @return an {@link ExportOutcome} with the export operation's outcome
   * @see ExportConfiguration
   */
  public static ExportOutcome export(FormDefinition formDef, ExportConfiguration configuration, boolean exportMedia) {
    long start = System.nanoTime();
    // Create an export tracker object with the total number of submissions we have to export
    long submissionCount = walk(formDef.getFormDir().resolve("instances"))
        .filter(UncheckedFiles::isInstanceDir)
        .count();
    ExportProcessTracker exportTracker = new ExportProcessTracker(formDef, submissionCount);
    exportTracker.start();

    // Compute and create the export directory
    Path exportDir = configuration.getExportDir().orElseThrow(() -> new BriefcaseException("No export dir defined"));
    createDirectories(exportDir);

    List<OutputFile> outputFiles = new ArrayList<>();
    outputFiles.add(OutputFile.mainFile(formDef, configuration, exportMedia));
    outputFiles.addAll(formDef.getModel().getRepeatableFields().stream().map(groupModel -> OutputFile.repeatFile(formDef, groupModel, configuration, exportMedia)).collect(Collectors.toList()));

    SubmissionParser
        .getListOfSubmissionFiles(formDef, configuration.getDateRange())
        .map(path -> SubmissionParser.parseSubmission(path, formDef.isFileEncryptedForm(), configuration.getPrivateKey()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(submission -> outputFiles.forEach(of -> of.append(submission)));

    outputFiles.forEach(OutputFile::persist);

    exportTracker.end();

    ExportOutcome exportOutcome = exportTracker.computeOutcome();
    if (exportOutcome == ALL_EXPORTED)
      EventBus.publish(ExportEvent.successForm(formDef, (int) exportTracker.total));

    if (exportOutcome == SOME_SKIPPED)
      EventBus.publish(ExportEvent.partialSuccessForm(formDef, (int) exportTracker.exported, (int) exportTracker.total));

    if (exportOutcome == ALL_SKIPPED)
      EventBus.publish(ExportEvent.failure(formDef, "All submissions have been skipped"));

    long end = System.nanoTime();
    LocalTime duration = LocalTime.ofNanoOfDay(end - start);
    log.info("Exported in {}", duration.format(DateTimeFormatter.ISO_TIME));
    return exportOutcome;
  }

  // TODO Invert the relation between Submission and OutputFile to make OutputFile a mapper of Submission to avoid having to aggregate all lines into the List
  static class OutputFile {
    private static final OffsetDateTime MIN_SUBMISSION_DATE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset());
    private final String header;
    private final Path output;
    private final boolean overwrite;
    private final List<Pair<OffsetDateTime, String>> linesBySubmissionDate = new ArrayList<>();
    private final Function<Submission, Pair<OffsetDateTime, String>> submissionMapper;

    OutputFile(String header, Path output, boolean overwrite, Function<Submission, Pair<OffsetDateTime, String>> submissionMapper) {
      this.header = header;
      this.output = output;
      this.overwrite = overwrite;
      this.submissionMapper = submissionMapper;
    }

    static OutputFile mainFile(FormDefinition formDefinition, ExportConfiguration configuration, boolean exportMedia) {
      Path output = configuration.getExportDir()
          .orElseThrow(BriefcaseException::new)
          .resolve(configuration.getExportFileName().orElse(stripIllegalChars(formDefinition.getFormName()) + ".csv"));
      Function<Submission, Pair<OffsetDateTime, String>> submissionMapper = submission -> Pair.of(
          submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
          getMainSubmissionLines(
              submission,
              formDefinition.getModel(),
              formDefinition.isFileEncryptedForm(),
              exportMedia,
              configuration.getExportMediaPath()
          )
      );
      return new OutputFile(
          getMainHeader(formDefinition.getModel(), formDefinition.isFileEncryptedForm()),
          output,
          configuration.getOverwriteExistingFiles().orElse(false),
          submissionMapper
      );
    }

    static OutputFile repeatFile(FormDefinition formDefinition, Model model, ExportConfiguration configuration, boolean exportMedia) {
      String repeatFileNameBase = configuration.getExportFileName()
          .map(UncheckedFiles::stripFileExtension)
          .orElse(stripIllegalChars(formDefinition.getFormName()));
      Path output = configuration.getExportDir()
          .orElseThrow(BriefcaseException::new)
          .resolve(repeatFileNameBase + "-" + model.getName() + ".csv");
      Function<Submission, Pair<OffsetDateTime, String>> submissionMapper = submission -> Pair.of(
          submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
          getRepeatCsvLine(
              model,
              submission.getElements(model.fqn()),
              exportMedia,
              configuration.getExportMediaPath(),
              submission.getInstanceId(),
              submission.getWorkingDir()
          )
      );
      return new OutputFile(
          getRepeatHeader(model),
          output,
          configuration.getOverwriteExistingFiles().orElse(false),
          submissionMapper
      );
    }

    public void append(Submission submission) {
      linesBySubmissionDate.add(submissionMapper.apply(submission));
    }

    void persist() {
      Stream<String> lines = linesBySubmissionDate.stream()
          // TODO Check why, oh why, we always get a null element in the list
          .filter(Objects::nonNull)
          .sorted(comparing(Pair::getLeft))
          .map(Pair::getRight);
      // TODO Optimize this and write in the file once
      if (Files.exists(output) && !overwrite)
        write(output, "\n".getBytes(), APPEND);
      else
        write(output, (header + "\n").getBytes(), CREATE, TRUNCATE_EXISTING);
      write(output, lines, APPEND);
    }
  }
}
