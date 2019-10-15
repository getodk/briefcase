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

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.StringUtils.sanitize;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.readFirstLine;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.toURI;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.walk;

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
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.ContainerHelper;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.submission.SubmissionKey;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExportToCsvScenario {
  private static final Logger log = LoggerFactory.getLogger(ExportToCsvScenario.class);
  private final AtomicInteger seq = new AtomicInteger(0);
  private final Container container;
  private final Path formDir;
  private final Path outputDir;
  private final FormDefinition formDef;
  private final Optional<String> instanceID;
  private final Locale localeBackup;
  private final TimeZone zoneBackup;
  private final FormMetadata formMetadata;

  private ExportToCsvScenario(Container container, Path formDir, Path outputDir, FormDefinition formDef, Optional<String> instanceID, Locale localeBackup, TimeZone zoneBackup, FormMetadata formMetadata) {
    this.container = container;
    this.formDir = formDir;
    this.outputDir = outputDir;
    this.formDef = formDef;
    this.instanceID = instanceID;
    this.localeBackup = localeBackup;
    this.zoneBackup = zoneBackup;
    this.formMetadata = formMetadata;
  }

  static ExportToCsvScenario setUp(String formName) {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    Container container = ContainerHelper.inMemory();

    Path sourceFormFile = getPath(formName + ".xml");
    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormFile);

    FormMetadata targetMetadata = sourceFormMetadata.withFormFile(container.workspace.buildFormFile(sourceFormMetadata));
    container.formMetadata.persist(targetMetadata);
    Optional<SubmissionMetadata> submissionMetadata = installForm(sourceFormMetadata, targetMetadata);
    submissionMetadata.ifPresent(container.submissionMetadata::persist);

    log.debug("Form dir: {}", targetMetadata.getFormDir());

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
        container,
        targetMetadata.getFormDir(),
        outputDir,
        FormDefinition.from(targetMetadata.getFormFile()),
        readInstanceId(formName),
        localeBackup,
        zoneBackup,
        targetMetadata
    );
  }

  static Path getPath(String fileName) {
    return maybeGetPath(fileName).orElseThrow(RuntimeException::new);
  }

  void tearDown() {
    deleteRecursive(container.workspace.get());
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
        .setExportAttachments(exportMedia)
        .setSplitSelectMultiples(splitSelectMultiples)
        .build();

    ExportToCsv.export(formMetadata, formDef, configuration, container.submissionMetadata, container.formMetadata);
  }

  void assertSameContent() {
    assertSameContent("");
  }

  void assertSameContent(String suffix) {
    String fileName = formDef.getFormId() + (suffix.isEmpty() ? "" : "-" + suffix) + ".csv.expected";
    String oldOutput = new String(readAllBytes(getPath(fileName)));
    String newOutput = new String(readAllBytes(outputDir.resolve("new").resolve(sanitize(formDef.getFormName()) + ".csv")));
    assertThat(newOutput, is(oldOutput));
  }

  void assertSameMedia() {
    assertSameMedia("");
  }

  void assertSameMedia(String suffix) {
    String fileName = formDef.getFormId() + "-media" + (suffix.isEmpty() ? "" : "-" + suffix);
    Path oldMediaPath = getPath(fileName);
    Path newMediaPath = outputDir.resolve("new").resolve("media");
    List<Path> oldMedia = walk(oldMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).filter(p -> Files.isRegularFile(p)).collect(toList());
    List<Path> newMedia = walk(newMediaPath).filter(p -> !p.getFileName().toString().startsWith(".git")).filter(p -> Files.isRegularFile(p)).collect(toList());
    assertThat(newMedia, hasSize(oldMedia.size()));
    oldMedia.stream().filter(Files::isRegularFile).forEach(path ->
        assertThat(readAllBytes(newMediaPath.resolve(oldMediaPath.relativize(path))), equalTo(readAllBytes(path)))
    );
  }

  void assertSameContentRepeats(String suffix, String... groupNames) {
    Arrays.asList(groupNames).forEach(groupName -> {
      String oldOutput = new String(readAllBytes(getPath(formDef.getFormId() + "-" + groupName + (suffix.isEmpty() ? "" : "-" + suffix) + ".csv.expected")));
      String newOutput = new String(readAllBytes(outputDir.resolve("new").resolve(sanitize(formDef.getFormName()) + "-" + groupName + ".csv")));
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
    container.submissionMetadata.persist(SubmissionMetadata.from(instanceFile, emptyList()));
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
    container.submissionMetadata.flush();
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
    container.submissionMetadata.persist(SubmissionMetadata.from(target, emptyList()));
  }

  private static Optional<Path> maybeGetPath(String fileName) {
    return Optional.ofNullable(ExportToCsvTest.class.getClassLoader().getResource("org/opendatakit/briefcase/operations/export/" + fileName))
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

  private static Optional<SubmissionMetadata> installForm(FormMetadata sourceFormMetadata, FormMetadata targetFormMetadata) {
    Path sourceFormFile = sourceFormMetadata.getFormFile();
    String baseSourceFilename = UncheckedFiles.stripFileExtension(sourceFormFile);

    createDirectories(targetFormMetadata.getFormDir());
    copy(sourceFormMetadata.getFormFile(), targetFormMetadata.getFormFile());

    // Prepare the instances directory
    createDirectories(targetFormMetadata.getSubmissionsDir());

    // Copy a submission and possibly other files
    Path submissionFile = sourceFormMetadata.getFormDir().resolve(baseSourceFilename + "-submission.xml");
    if (Files.exists(submissionFile)) {
      String instanceId = SubmissionKey.extractInstanceId(XmlElement.from(submissionFile)).orElseThrow();
      createDirectories(targetFormMetadata.getSubmissionDir(instanceId));
      Path targetSubmissionFile = targetFormMetadata.getSubmissionFile(instanceId);
      copy(submissionFile, targetSubmissionFile);

      List<Pair<Path, Path>> filesToInstall = list(submissionFile.getParent())
          .filter(Files::isRegularFile)
          .filter(isRelatedToForm(baseSourceFilename))
          .filter(not(ExportToCsvScenario::isSubmissionFile))
          .filter(not(ExportToCsvScenario::isPemFile))
          .filter(not(ExportToCsvScenario::isTemplateFile))
          .filter(not(ExportToCsvScenario::isExpectedContentsFile))
          .map(sourceAttachmentFile -> Pair.of(
              sourceAttachmentFile,
              targetFormMetadata.getSubmissionAttachmentFile(instanceId, sourceAttachmentFile.getFileName().toString().substring(baseSourceFilename.length() + 1))
          ))
          .collect(toList());

      filesToInstall.forEach(pair -> copy(pair.getLeft(), pair.getRight()));

      List<Path> attachmentFilenames = filesToInstall.stream()
          .map(Pair::getRight)
          .filter(not(ExportToCsvScenario::isEncryptedSubmissionFile))
          .map(Path::getFileName)
          .sorted()
          .collect(toList());

      return Optional.of(SubmissionMetadata.from(targetSubmissionFile, attachmentFilenames));
    }
    return Optional.empty();
  }

  private static boolean isSubmissionFile(Path path) {
    return path.getFileName().toString().endsWith("-submission.xml");
  }

  private static boolean isEncryptedSubmissionFile(Path path) {
    return path.getFileName().toString().endsWith("submission.xml.enc");
  }

  private static boolean isExpectedContentsFile(Path path) {
    return path.getFileName().toString().endsWith(".expected");
  }

  private static boolean isTemplateFile(Path path) {
    return path.getFileName().toString().endsWith(".tpl");
  }

  private static boolean isPemFile(Path path) {
    return path.getFileName().toString().endsWith(".pem");
  }

  private static Predicate<Path> isRelatedToForm(String formName) {
    return file -> file.getFileName().toString().startsWith(formName + "-");
  }
}
