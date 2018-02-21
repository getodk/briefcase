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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.opendatakit.briefcase.export.CsvMapper.getMainHeader;
import static org.opendatakit.briefcase.export.CsvMapper.getMainSubmissionLines;
import static org.opendatakit.briefcase.export.CsvMapper.getRepeatCsvLine;
import static org.opendatakit.briefcase.export.CsvMapper.getRepeatHeader;
import static org.opendatakit.briefcase.reused.Lists.prepend;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Lists;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.util.Pair;

public class ExportToCsv {
  /**
   * This method will export a form's submissions into some CSV files.
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
    // Read the form's model
    Model mainModel = Model.from(formDef);

    // Parse submissions
    List<Submission> submissions = SubmissionParser.parseAllInFormDir(
        formDef.getFormDir(),
        formDef.isFileEncryptedForm(),
        configuration.getPrivateKey(),
        configuration.getDateRange()
    );

    // Compute and create the export directory
    Path exportDir = configuration.getExportDir().orElseThrow(() -> new BriefcaseException("No export dir defined"));
    createDirectories(exportDir);

    // Write a CSV for the main form's contents
    writeMainCsv(
        configuration,
        exportMedia,
        mainModel,
        submissions,
        exportDir,
        formDef.isFileEncryptedForm(),
        formDef.getFormName()
    );

    // Write CSV files for each repeat group
    writeRepeatCsvs(
        configuration,
        exportMedia,
        mainModel,
        submissions,
        exportDir,
        formDef.getFormName()
    );

    // Check exported submissions count
    long submissionCount = walk(formDef.getFormDir().resolve("instances"))
        .filter(UncheckedFiles::isInstanceDir)
        .count();

    // Compute an outcome
    return submissions.size() == submissionCount
        ? ExportOutcome.ALL_EXPORTED
        : submissions.size() < submissionCount
        ? ExportOutcome.SOME_SKIPPED
        : ExportOutcome.ALL_SKIPPED;
  }

  private static void writeMainCsv(ExportConfiguration configuration, boolean exportMedia, Model mainModel, List<Submission> submissions, Path exportDir, boolean isEncrypted, String formName) {
    Stream<String> csvLines = submissions.stream()
        .map(submission -> getMainSubmissionLines(
            submission,
            mainModel,
            isEncrypted,
            exportMedia,
            configuration.getExportMediaPath()
        ))
        // We sort the lines using the submission date attached to each line
        .sorted(comparing(form -> form.left))
        .map(form -> form.right);

    write(
        exportDir.resolve(stripIllegalChars(formName) + ".csv"),
        configuration.getOverwriteExistingFiles().orElse(false),
        getMainHeader(mainModel, isEncrypted),
        csvLines
    );
  }

  private static void writeRepeatCsvs(ExportConfiguration configuration, boolean exportMedia, Model mainModel, List<Submission> submissions, Path exportDir, String formName) {
    // To avoid having to read all submissions for each repeat group defined on the form,
    // we make one pass and accumulate lines linked to their corresponding group
    submissions.stream()
        .flatMap(submission -> getRepeatCsvLines(submission, mainModel, configuration, exportMedia).stream())
        // Once those lines are generated, we can merge them. This will produce a map entry
        // for each repeat group with the lines from all the submissions
        .collect(toMap(Pair::getLeft, Pair::getRight, Lists::concat))
        .forEach((groupModel, lines) -> write(
            exportDir.resolve(stripIllegalChars(formName) + "-" + groupModel.getName() + ".csv"),
            configuration.getOverwriteExistingFiles().orElse(false),
            getRepeatHeader(groupModel),
            lines.stream()
        ));
  }

  private static List<Pair<Model, List<String>>> getRepeatCsvLines(Submission submission, Model model, ExportConfiguration configuration, boolean exportMedia) {
    // This map will serve as an index to get the corresponding submission element
    // using the field's FQN, which is the same in both the Model and XmlElement instances
    Map<String, List<XmlElement>> elementsByFqn = submission.root.getChildrenIndex();

    return model.getRepeatableFields().stream().map(groupModel -> {
      List<String> repeatCsvLine = getRepeatCsvLine(
          groupModel,
          elementsByFqn.get(groupModel.fqn()),
          exportMedia,
          configuration.getExportMediaPath(),
          submission.getInstanceId(),
          submission.workingDir
      );
      return Pair.of(groupModel, repeatCsvLine);
    }).collect(toList());
  }

  private static void write(Path outputFile, boolean overwrite, String header, Stream<String> csvLines) {
    // This method will take care of appending or overwriting
    // the output file depending on given arguments
    if (Files.exists(outputFile) && !overwrite)
      UncheckedFiles.write(outputFile, csvLines, APPEND);
    else
      UncheckedFiles.write(outputFile, prepend(header, csvLines), CREATE, TRUNCATE_EXISTING);
  }

}
