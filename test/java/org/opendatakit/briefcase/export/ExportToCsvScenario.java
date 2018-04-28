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

import static java.time.ZoneId.systemDefault;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readFirstLine;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Security;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.OldExportToCsv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExportToCsvScenario {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsvScenario.class);
  private final AtomicInteger seq = new AtomicInteger(0);
  private final Path formDir;
  private final Path outputDir;
  private final FormDefinition formDef;
  private final BriefcaseFormDefinition oldFormDef;
  private final Optional<String> instanceID;
  private final Locale localeBackup;
  private final TimeZone zoneBackup;

  ExportToCsvScenario(Path formDir, Path outputDir, FormDefinition formDef, BriefcaseFormDefinition oldFormDef, Optional<String> instanceID, Locale localeBackup, TimeZone zoneBackup) {
    this.formDir = formDir;
    this.outputDir = outputDir;
    this.formDef = formDef;
    this.oldFormDef = oldFormDef;
    this.instanceID = instanceID;
    this.localeBackup = localeBackup;
    this.zoneBackup = zoneBackup;
  }

  static ExportToCsvScenario setUp(String formName) {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    Path formDir = createTempDirectory("briefcase_export_test_form_");
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
        formDir,
        outputDir,
        FormDefinition.from(formFile),
        buildOldFormDefinition(formFile),
        readInstanceId(formName),
        localeBackup,
        zoneBackup
    );
  }

  static Path getPath(String fileName) {
    return maybeGetPath(fileName).orElseThrow(RuntimeException::new);
  }

  void tearDown() {
    deleteRecursive(formDir);
    deleteRecursive(outputDir);
    Locale.setDefault(localeBackup);
    TimeZone.setDefault(zoneBackup);
  }

  void runOldExport() {
    runOldExport(true, true, null, null, null);
  }

  void runOldExport(PrivateKey privateKey) {
    runOldExport(true, true, null, null, privateKey);
  }

  void runOldExport(LocalDate startDate, LocalDate endDate) {
    runOldExport(true, true, startDate, endDate, null);
  }

  void runOldExport(boolean overwrite) {
    runOldExport(overwrite, true, null, null, null);
  }

  void runOldExport(boolean overwrite, boolean exportMedia) {
    runOldExport(overwrite, exportMedia, null, null, null);
  }

  void runOldExport(boolean overwrite, boolean exportMedia, LocalDate startDate, LocalDate endDate, PrivateKey privateKey) {
    oldFormDef.setPrivateKey(privateKey);
    new OldExportToCsv(
        new TerminationFuture(),
        outputDir.resolve("old").toFile(),
        oldFormDef,
        oldFormDef.getFormName(),
        exportMedia,
        overwrite,
        Optional.ofNullable(startDate).map(ld -> Date.from(ld.atStartOfDay(systemDefault()).toInstant())).orElse(null),
        Optional.ofNullable(endDate).map(ld -> Date.from(ld.atStartOfDay(systemDefault()).toInstant())).orElse(null)
    ).doAction();
  }


  void runNewExport() {
    runNewExport(true, true, null, null, null);
  }

  void runNewExport(Path pemFile) {
    runNewExport(true, true, null, null, pemFile);
  }

  void runNewExport(boolean overwrite) {
    runNewExport(overwrite, true, null, null, null);
  }

  void runNewExport(boolean overwrite, boolean exportMedia) {
    runNewExport(overwrite, exportMedia, null, null, null);
  }

  void runNewExport(LocalDate startDate, LocalDate endDate) {
    runNewExport(true, true, startDate, endDate, null);
  }

  void runNewExport(boolean overwrite, boolean exportMedia, LocalDate startDate, LocalDate endDate, Path pemFile) {
    ExportConfiguration configuration = new ExportConfiguration(
        Optional.of(outputDir.resolve("new")),
        Optional.ofNullable(pemFile),
        Optional.ofNullable(startDate),
        Optional.ofNullable(endDate),
        Optional.of(false),
        Optional.empty(),
        Optional.of(overwrite)
    );
    ExportToCsv.export(formDef, configuration, exportMedia);
  }

  void assertSameContent() {
    String oldOutput = new String(readAllBytes(outputDir.resolve("old").resolve(stripIllegalChars(formDef.getFormName()) + ".csv")));
    String newOutput = new String(readAllBytes(outputDir.resolve("new").resolve(stripIllegalChars(formDef.getFormName()) + ".csv")));
    assertThat(newOutput, is(oldOutput));
  }

  void assertSameMedia() {
    Path oldMediaPath = outputDir.resolve("old").resolve("media");
    Path newMediaPath = outputDir.resolve("new").resolve("media");
    List<Path> oldMedia = walk(oldMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).collect(Collectors.toList());
    List<Path> newMedia = walk(newMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).collect(Collectors.toList());
    assertThat(newMedia, hasSize(oldMedia.size()));
    oldMedia.stream().filter(Files::isRegularFile).forEach(path ->
        assertThat(readAllBytes(newMediaPath.resolve(oldMediaPath.relativize(path))), equalTo(readAllBytes(path)))
    );
  }

  void assertSameContentRepeats(String... groupNames) {
    Arrays.asList(groupNames).forEach(groupName -> {
      String oldOutput = new String(readAllBytes(outputDir.resolve("old").resolve(stripIllegalChars(formDef.getFormName()) + "_" + groupName + ".csv")));
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
    write(instanceFile, instanceContent.getBytes());
  }

  void createOutputFile(String dir, String file) {
    createDirectories(outputDir.resolve("old").resolve(dir));
    write(outputDir.resolve("old").resolve(dir).resolve(file), "Some content".getBytes());
    createDirectories(outputDir.resolve("new").resolve(dir));
    write(outputDir.resolve("new").resolve(dir).resolve(file), "Some content".getBytes());
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
    return maybeGetPath(formName + "_submission.xml")
        .filter(Files::exists)
        .map(ExportToCsvScenario::readInstanceId);
  }

  private static String readInstanceId(Path path) {
    return readFirstLine(path).split("instanceID=\"")[1].split("\"")[0];
  }

  private static Path installForm(Path formDir, final String formName) {
    // Locate and copy the form definition
    Path sourceForm = getPath(formName + ".xml");
    Path targetForm = formDir.resolve("form.xml");
    copy(sourceForm, targetForm);

    // Prepare the instances directory
    Path instancesDir = formDir.resolve("instances");
    createDirectories(instancesDir);

    // Copy a submission and possibly other files
    maybeGetPath(formName + "_submission.xml")
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
              .forEach(file -> {
                Path target = submissionDir.resolve(file.getFileName().toString().substring(formName.length() + 1));
                log.debug("Install " + submissionDir.getFileName() + "/" + target.getFileName());
                copy(file, target);
              });
        });

    return targetForm;
  }

  private static Predicate<Path> isTemplateFile() {
    return file -> file.getFileName().toString().endsWith(".tpl");
  }

  private static Predicate<Path> isPemFile() {
    return file -> file.getFileName().toString().endsWith(".pem");
  }

  private static Predicate<Path> isRelatedToForm(String formName) {
    return file -> file.getFileName().toString().startsWith(formName + "_");
  }

  private static BriefcaseFormDefinition buildOldFormDefinition(Path formFile) {
    try {
      return new BriefcaseFormDefinition(formFile.getParent().toFile(), formFile.toFile());
    } catch (BadFormDefinition e) {
      throw new RuntimeException(e);
    }
  }
}
