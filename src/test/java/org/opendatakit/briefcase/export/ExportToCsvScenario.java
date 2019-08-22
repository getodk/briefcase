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

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.BriefcaseFormDefinition.resolveAgainstBriefcaseDefn;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readFirstLine;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExportToCsvScenario {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsvScenario.class);
  private final AtomicInteger seq = new AtomicInteger(0);
  private final Path briefcaseDir;
  private final Path formDir;
  private final Path outputDir;
  private final FormDefinition formDef;
  private final FormStatus formStatus;
  private final Optional<String> instanceID;
  private final Locale localeBackup;
  private final TimeZone zoneBackup;

  ExportToCsvScenario(Path briefcaseDir, Path formDir, Path outputDir, FormDefinition formDef, FormStatus formStatus, Optional<String> instanceID, Locale localeBackup, TimeZone zoneBackup) {
    this.briefcaseDir = briefcaseDir;
    this.formDir = formDir;
    this.outputDir = outputDir;
    this.formDef = formDef;
    this.formStatus = formStatus;
    this.instanceID = instanceID;
    this.localeBackup = localeBackup;
    this.zoneBackup = zoneBackup;
  }

  static ExportToCsvScenario setUp(String formName) {
    try {
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
      Path briefcaseDir = createTempDirectory("briefcase");

      Path sourceFormFile = getPath(formName + ".xml");
      FormStatus formStatus = new FormStatus(resolveAgainstBriefcaseDefn(sourceFormFile.toFile(), true, briefcaseDir.toFile()));

      Path formDir = formStatus.getFormDir(briefcaseDir);
      createDirectories(formDir);
      log.debug("Form dir: {}", formDir);
      Path formFile = installForm(formDir, formName);

      Path outputDir = createTempDirectory("briefcase_export_test_output_");
      log.debug("Output dir: {}", outputDir);
      createDirectories(outputDir.resolve("old"));
      createDirectories(outputDir.resolve("new"));

      Locale localeBackup = Locale.getDefault();
      Locale.setDefault(Locale.forLanguageTag("en_US")); // This Locale will force us to escape dates

      // We will run tests on UTC
      TimeZone zoneBackup = TimeZone.getDefault();
      TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

      return new ExportToCsvScenario(
          briefcaseDir,
          formDir,
          outputDir,
          FormDefinition.from(formFile),
          formStatus,
          readInstanceId(formName),
          localeBackup,
          zoneBackup
      );
    } catch (BadFormDefinition badFormDefinition) {
      throw new RuntimeException(badFormDefinition);
    }
  }

  static Path getPath(String fileName) {
    return maybeGetPath(fileName).orElseThrow(RuntimeException::new);
  }

  void tearDown() {
    deleteRecursive(briefcaseDir);
    deleteRecursive(outputDir);
    Locale.setDefault(localeBackup);
    TimeZone.setDefault(zoneBackup);
  }

  // TODO Think of a better way to customize running these exports. Maybe use a Configuration object and a builder
  void runExportExplodedChoiceLists() {
    runExport(true, true, null, null, null, true);
  }

  void runExport() {
    runExport(true, true, null, null, null, false);
  }

  void runExport(Path pemFile) {
    runExport(true, true, null, null, pemFile, false);
  }

  void runExport(boolean overwrite) {
    runExport(overwrite, true, null, null, null, false);
  }

  void runExport(boolean overwrite, boolean exportMedia) {
    runExport(overwrite, exportMedia, null, null, null, false);
  }

  void runExport(LocalDate startDate, LocalDate endDate) {
    runExport(true, true, startDate, endDate, null, false);
  }

  void runExport(boolean overwrite, boolean exportMedia, LocalDate startDate, LocalDate endDate, Path pemFile, boolean splitSelectMultiples) {
    ExportConfiguration configuration = ExportConfiguration.Builder.empty()
        .setExportDir(outputDir.resolve("new"))
        .setPemFile(pemFile)
        .setStartDate(startDate)
        .setEndDate(endDate)
        .setOverwriteFiles(overwrite)
        .setExportMedia(exportMedia)
        .setSplitSelectMultiples(splitSelectMultiples)
        .build();
    FormKey formKey = FormKey.of(formDef.getFormName(), formDef.getFormId());
    FormMetadata formMetadata = new FormMetadata(
        formKey,
        formDef.getFormDir(),
        Paths.get(stripIllegalChars(formDef.getFormName()) + ".xml"),
        true,
        Cursor.empty(),
        Optional.empty()
    );
    ExportToCsv.export(new InMemoryFormMetadataAdapter(), formMetadata, formStatus, formDef, briefcaseDir, configuration);
  }

  void assertSameContent() {
    assertSameContent("");
  }

  void assertSameContent(String suffix) {
    String oldOutput = new String(readAllBytes(getPath(formDef.getFormId() + (suffix.isEmpty() ? "" : "-" + suffix) + ".csv.expected")));
    String newOutput = new String(readAllBytes(outputDir.resolve("new").resolve(stripIllegalChars(formDef.getFormName()) + ".csv")));
    assertThat(newOutput, is(oldOutput));
  }

  void assertSameMedia() {
    assertSameMedia("");
  }

  void assertSameMedia(String suffix) {
    Path oldMediaPath = getPath(formDef.getFormId() + "-media" + (suffix.isEmpty() ? "" : "-" + suffix));
    Path newMediaPath = outputDir.resolve("new").resolve("media");
    List<Path> oldMedia = walk(oldMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).filter(p -> Files.isRegularFile(p)).collect(Collectors.toList());
    List<Path> newMedia = walk(newMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).filter(p -> Files.isRegularFile(p)).collect(Collectors.toList());
    assertThat(newMedia, hasSize(oldMedia.size()));
    oldMedia.stream().filter(Files::isRegularFile).forEach(path ->
        assertThat(readAllBytes(newMediaPath.resolve(oldMediaPath.relativize(path))), equalTo(readAllBytes(path)))
    );
  }

  void assertSameContentRepeats(String suffix, String... groupNames) {
    Arrays.asList(groupNames).forEach(groupName -> {
      String oldOutput = new String(readAllBytes(getPath(formDef.getFormId() + "-" + groupName + (suffix.isEmpty() ? "" : "-" + suffix) + ".csv.expected")));
      String newOutput = new String(readAllBytes(outputDir.resolve("new").resolve(stripIllegalChars(formDef.getFormName()) + "-" + groupName + ".csv")));
      assertThat(newOutput, is(oldOutput));
    });
  }

  Path getSubmissionDir() {
    return formDir.resolve("instances").resolve(instanceID.orElseThrow(RuntimeException::new).replaceAll(":", ""));
  }

  void createInstance(String submissionTpl, LocalDate submissionDate) {
    createInstance(submissionTpl, submissionDate, "some value");
  }

  void createInstance(String submissionTpl, LocalDate submissionDate, String value) {
    String formattedSubmissionDate = submissionDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).format(ISO_OFFSET_DATE_TIME);
    String instanceId = String.format("uuid00000000-0000-0000-0000-%012d", seq.getAndIncrement());
    String instanceContent = String.format(submissionTpl, instanceId, formattedSubmissionDate, formattedSubmissionDate, value);
    Path instanceDir = formDir.resolve("instances").resolve(instanceId);
    createDirectories(instanceDir);
    Path instanceFile = instanceDir.resolve("submission.xml");
    UncheckedFiles.write(instanceFile, instanceContent);
  }

  void createOutputFile(String dir, String file) {
    createDirectories(outputDir.resolve("old").resolve(dir));
    UncheckedFiles.write(outputDir.resolve("old").resolve(dir).resolve(file), "Some content");
    createDirectories(outputDir.resolve("new").resolve(dir));
    UncheckedFiles.write(outputDir.resolve("new").resolve(dir).resolve(file), "Some content");
  }

  void assertNoOutputMediaDir() {
    assertThat(Files.exists(outputDir.resolve("old").resolve("media")), is(false));
    assertThat(Files.exists(outputDir.resolve("new").resolve("media")), is(false));
  }

  String readFile(String fileName) {
    return new String(readAllBytes(getPath(fileName)));
  }

  void clearSubmissions() {
    Path instancesDir = formDir.resolve("instances");
    if (Files.exists(instancesDir))
      deleteRecursive(instancesDir);
    UncheckedFiles.createDirectories(instancesDir);
  }

  void installSubmission(String fileName) {
    Path source = getPath(fileName);
    String instanceId = readInstanceId(source);
    Path instancesDir = formDir.resolve("instances");
    Path submissionDir = instancesDir.resolve(instanceId.replace(":", ""));
    createDirectories(submissionDir);

    Path target = submissionDir.resolve("submission.xml");
    log.debug("Install " + submissionDir.getFileName() + "/" + target.getFileName());
    copy(source, target);
  }

  private static Optional<Path> maybeGetPath(String fileName) {
    return Optional.ofNullable(ExportToCsvTest.class.getClassLoader().getResource("org/opendatakit/briefcase/export/" + fileName))
        .map(url -> Paths.get(toURI(url)));
  }

  private static Optional<String> readInstanceId(String formName) {
    return maybeGetPath(formName + "-submission.xml")
        .filter(Files::exists)
        .map(ExportToCsvScenario::readInstanceId);
  }

  private static String readInstanceId(Path path) {
    return readFirstLine(path).split("instanceID=\"")[1].split("\"")[0];
  }

  private static Path installForm(Path formDir, final String formName) {
    // Locate and copy the form definition
    Path sourceForm = getPath(formName + ".xml");
    Path targetForm = formDir.resolve(sourceForm.getFileName());
    copy(sourceForm, targetForm);

    // Prepare the instances directory
    Path instancesDir = formDir.resolve("instances");
    createDirectories(instancesDir);

    // Copy a submission and possibly other files
    maybeGetPath(formName + "-submission.xml")
        .filter(Files::exists)
        .ifPresent(path -> {
          // Read the submission's instance ID
          String instanceId = readInstanceId(path);

          // Create a dir for this submission
          Path submissionDir = instancesDir.resolve(instanceId.replace(":", ""));
          createDirectories(submissionDir);

          // We will copy every file we find that is prefixed with the
          // name of the form we're installing, ignoring PEM files and templates
          walk(path.getParent())
              .filter(isRelatedToForm(formName))
              .filter(isPemFile().negate())
              .filter(isTemplateFile().negate())
              .filter(isExpectedContentsFile().negate())
              .forEach(file -> {
                Path target = submissionDir.resolve(file.getFileName().toString().substring(formName.length() + 1));
                log.debug("Install " + submissionDir.getFileName() + "/" + target.getFileName());
                copy(file, target);
              });
        });

    return targetForm;
  }

  private static Predicate<Path> isExpectedContentsFile() {
    return file -> file.getFileName().toString().endsWith(".expected");
  }

  private static Predicate<Path> isTemplateFile() {
    return file -> file.getFileName().toString().endsWith(".tpl");
  }

  private static Predicate<Path> isPemFile() {
    return file -> file.getFileName().toString().endsWith(".pem");
  }

  private static Predicate<Path> isRelatedToForm(String formName) {
    return file -> file.getFileName().toString().startsWith(formName + "-");
  }
}
