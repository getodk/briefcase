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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newInputStream;
import static org.opendatakit.briefcase.delivery.ui.reused.filsystem.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.delivery.ui.reused.filsystem.FileChooser.isUnderODKFolder;
import static org.opendatakit.briefcase.reused.api.StringUtils.stripIllegalChars;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.exists;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.bouncycastle.openssl.PEMReader;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Json;
import org.opendatakit.briefcase.reused.api.StringUtils;
import org.opendatakit.briefcase.reused.api.TriStateBoolean;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.OverridableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportConfiguration {
  private static final Logger log = LoggerFactory.getLogger(ExportConfiguration.class);
  private final Optional<String> exportFileName;
  private final Optional<Path> exportDir;
  private final Optional<Path> pemFile;
  private final DateRange dateRange;
  private final OverridableBoolean pullBefore;
  private final OverridableBoolean overwriteFiles;
  private final OverridableBoolean exportMedia;
  private final OverridableBoolean splitSelectMultiples;
  private final OverridableBoolean includeGeoJsonExport;
  private final OverridableBoolean removeGroupNames;
  private final OverridableBoolean smartAppend;

  private ExportConfiguration(Optional<String> exportFileName, Optional<Path> exportDir, Optional<Path> pemFile, DateRange dateRange, OverridableBoolean pullBefore, OverridableBoolean overwriteFiles, OverridableBoolean exportMedia, OverridableBoolean splitSelectMultiples, OverridableBoolean includeGeoJsonExport, OverridableBoolean removeGroupNames, OverridableBoolean smartAppend) {
    this.exportFileName = exportFileName;
    this.exportDir = exportDir;
    this.pemFile = pemFile;
    this.dateRange = dateRange;
    this.pullBefore = pullBefore;
    this.overwriteFiles = overwriteFiles;
    this.exportMedia = exportMedia;
    this.splitSelectMultiples = splitSelectMultiples;
    this.includeGeoJsonExport = includeGeoJsonExport;
    this.removeGroupNames = removeGroupNames;
    this.smartAppend = smartAppend;
  }

  public static ExportConfiguration from(JsonNode root) {
    return new ExportConfiguration(
        Json.get(root, "exportFilename").map(JsonNode::asText),
        Json.get(root, "exportDir").map(JsonNode::asText).map(Paths::get),
        Json.get(root, "pemFile").map(JsonNode::asText).map(Paths::get),
        Json.get(root, "dateRange").flatMap(DateRange::from).orElse(DateRange.empty()),
        Json.get(root, "startFromLast").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "overwriteFiles").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "exportMedia").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "splitSelectMultiples").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "includeGeoJsonExport").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "removeGroupNames").map(OverridableBoolean::from).orElse(OverridableBoolean.empty()),
        Json.get(root, "smartAppend").map(OverridableBoolean::from).orElse(OverridableBoolean.empty())
    );
  }

  private static Optional<PrivateKey> readPemFile(Path pemFile) {
    try (InputStream is = newInputStream(pemFile);
         InputStreamReader isr = new InputStreamReader(is, UTF_8);
         BufferedReader br = new BufferedReader(isr);
         PEMReader pr = new PEMReader(br)
    ) {
      Object o = pr.readObject();
      if (o == null) {
        log.warn("The supplied file is not in PEM format");
        return Optional.empty();
      }

      if (o instanceof KeyPair)
        return Optional.of(((KeyPair) o).getPrivate());

      if (o instanceof PrivateKey)
        return Optional.of(((PrivateKey) o));

      log.warn("The supplied file does not contain a private key");
      return Optional.empty();
    } catch (IOException e) {
      throw new BriefcaseException("Briefcase can't read the pem file", e);
    }
  }

  public Path getExportDir() {
    return exportDir.orElseThrow(BriefcaseException::new);
  }

  public Path getPemFile() {
    return pemFile.orElseThrow(BriefcaseException::new);
  }

  public boolean isPemFilePresent() {
    return pemFile.isPresent();
  }

  public boolean resolvePullBefore() {
    return pullBefore.resolve(false);
  }

  boolean resolveExportMedia() {
    return exportMedia.resolve(true);
  }

  boolean resolveOverwriteExistingFiles() {
    return overwriteFiles.resolve(false);
  }

  boolean resolveSplitSelectMultiples() {
    return splitSelectMultiples.resolve(false);
  }

  public boolean resolveIncludeGeoJsonExport() {
    return includeGeoJsonExport.resolve(false);
  }

  boolean resolveRemoveGroupNames() {
    return removeGroupNames.resolve(false);
  }

  boolean resolveSmartAppend() {
    return smartAppend.resolve(false);
  }

  public OverridableBoolean getPullBefore() {
    return pullBefore;
  }

  public OverridableBoolean getOverwriteFiles() {
    return overwriteFiles;
  }

  public OverridableBoolean getExportMedia() {
    return exportMedia;
  }

  public OverridableBoolean getSplitSelectMultiples() {
    return splitSelectMultiples;
  }

  public OverridableBoolean getIncludeGeoJsonExport() {
    return includeGeoJsonExport;
  }

  public OverridableBoolean getRemoveGroupNames() {
    return removeGroupNames;
  }

  public OverridableBoolean getSmartAppend() {
    return smartAppend;
  }

  public void ifExportDirPresent(Consumer<Path> consumer) {
    exportDir.ifPresent(consumer);
  }

  public void ifPemFilePresent(Consumer<Path> consumer) {
    pemFile.ifPresent(consumer);
  }

  public boolean isEmpty() {
    return exportDir.isEmpty()
        && pemFile.isEmpty()
        && dateRange.isEmpty()
        && pullBefore.isEmpty()
        && overwriteFiles.isEmpty()
        && exportMedia.isEmpty()
        && splitSelectMultiples.isEmpty()
        && includeGeoJsonExport.isEmpty()
        && removeGroupNames.isEmpty()
        && smartAppend.isEmpty();
  }

  public boolean isValid() {
    return exportDir.isPresent();
  }

  public ExportConfiguration fallingBackTo(ExportConfiguration defaultConfiguration) {
    return Builder.empty()
        .setExportFilename(exportFileName.isPresent() ? exportFileName : defaultConfiguration.exportFileName)
        .setExportDir(exportDir.isPresent() ? exportDir : defaultConfiguration.exportDir)
        .setPemFile(pemFile.isPresent() ? pemFile : defaultConfiguration.pemFile)
        .setDateRange(!dateRange.isEmpty() ? dateRange : defaultConfiguration.dateRange)
        .setPullBefore(pullBefore.fallingBackTo(defaultConfiguration.pullBefore))
        .setOverwriteFiles(overwriteFiles.fallingBackTo(defaultConfiguration.overwriteFiles))
        .setExportMedia(exportMedia.fallingBackTo(defaultConfiguration.exportMedia))
        .setSplitSelectMultiples(splitSelectMultiples.fallingBackTo(defaultConfiguration.splitSelectMultiples))
        .setIncludeGeoJsonExport(includeGeoJsonExport.fallingBackTo(defaultConfiguration.includeGeoJsonExport))
        .setRemoveGroupNames(removeGroupNames.fallingBackTo(defaultConfiguration.removeGroupNames))
        .setSmartAppend(smartAppend.fallingBackTo(defaultConfiguration.smartAppend))
        .build();
  }

  Optional<PrivateKey> getPrivateKey() {
    return pemFile.flatMap(ExportConfiguration::readPemFile);
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  Path getExportMediaPath() {
    return exportDir.map(dir -> dir.resolve("media")).orElseThrow(() -> new BriefcaseException("No export dir configured"));
  }

  String getFilenameBase(String formName) {
    return exportFileName
        .map(UncheckedFiles::stripFileExtension)
        .map(StringUtils::stripIllegalChars)
        .orElse(stripIllegalChars(formName));
  }

  Path getErrorsDir(String formName) {
    return exportDir.map(dir -> dir.resolve(stripIllegalChars(formName) + " - errors")).orElseThrow(BriefcaseException::new);
  }

  Path getAuditPath(String formName) {
    return exportDir.orElseThrow(BriefcaseException::new).resolve(formName + " - audit.csv");
  }

  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("exportFilename", exportFileName.map(Object::toString).orElse(null));
    root.put("exportDir", exportDir.map(Object::toString).orElse(null));
    root.put("pemFile", pemFile.map(Object::toString).orElse(null));
    root.putObject("dateRange").setAll(dateRange.asJson(mapper));
    root.putObject("startFromLast").setAll(pullBefore.asJson(mapper));
    root.putObject("overwriteFiles").setAll(overwriteFiles.asJson(mapper));
    root.putObject("exportMedia").setAll(exportMedia.asJson(mapper));
    root.putObject("splitSelectMultiples").setAll(splitSelectMultiples.asJson(mapper));
    root.putObject("includeGeoJsonExport").setAll(includeGeoJsonExport.asJson(mapper));
    root.putObject("removeGroupNames").setAll(removeGroupNames.asJson(mapper));
    root.putObject("smartAppend").setAll(smartAppend.asJson(mapper));
    return root;
  }

  @Override
  public String toString() {
    return "ExportConfiguration{" +
        "exportDir=" + exportDir +
        ", pemFile=" + pemFile +
        ", dateRange=" + dateRange +
        ", pullBefore=" + pullBefore +
        ", overwriteFiles=" + overwriteFiles +
        ", exportMedia=" + exportMedia +
        ", splitSelectMultiples=" + splitSelectMultiples +
        ", includeGeoJsonExport=" + includeGeoJsonExport +
        ", removeGroupNames=" + removeGroupNames +
        ", smartAppend=" + smartAppend +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ExportConfiguration that = (ExportConfiguration) o;
    return Objects.equals(exportDir, that.exportDir) &&
        Objects.equals(pemFile, that.pemFile) &&
        Objects.equals(dateRange, that.dateRange) &&
        Objects.equals(pullBefore, that.pullBefore) &&
        Objects.equals(overwriteFiles, that.overwriteFiles) &&
        Objects.equals(exportMedia, that.exportMedia) &&
        Objects.equals(splitSelectMultiples, that.splitSelectMultiples) &&
        Objects.equals(includeGeoJsonExport, that.includeGeoJsonExport) &&
        Objects.equals(removeGroupNames, that.removeGroupNames) &&
        Objects.equals(smartAppend, that.smartAppend);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exportDir, pemFile, dateRange, pullBefore, overwriteFiles, exportMedia, splitSelectMultiples, includeGeoJsonExport, removeGroupNames, smartAppend);
  }

  public static class Builder {
    static final Consumer<String> NO_OP = __ -> { };
    private String exportFilename;
    private Path exportDir;
    private Path pemFile;
    private DateRange dateRange = DateRange.empty();
    private OverridableBoolean pullBefore = OverridableBoolean.empty();
    private OverridableBoolean overwriteFiles = OverridableBoolean.empty();
    private OverridableBoolean exportMedia = OverridableBoolean.empty();
    private OverridableBoolean splitSelectMultiples = OverridableBoolean.empty();
    private OverridableBoolean includeGeoJsonExport = OverridableBoolean.empty();
    private OverridableBoolean removeGroupNames = OverridableBoolean.empty();
    private OverridableBoolean smartAppend = OverridableBoolean.empty();

    public static Builder empty() {
      return new Builder();
    }

    public ExportConfiguration build() {
      return new ExportConfiguration(
          Optional.ofNullable(exportFilename),
          Optional.ofNullable(exportDir),
          Optional.ofNullable(pemFile),
          dateRange,
          pullBefore,
          overwriteFiles,
          exportMedia,
          splitSelectMultiples,
          includeGeoJsonExport,
          removeGroupNames,
          smartAppend
      );
    }

    public Builder setExportFilename(String fileName) {
      return setExportFilename(Optional.ofNullable(fileName));
    }

    Builder setExportFilename(Optional<String> fileName) {
      exportFilename = fileName.orElse(null);
      return this;
    }

    public Builder setExportDir(Path path) {
      return setExportDir(Optional.ofNullable(path), NO_OP);
    }

    public Builder setExportDir(Optional<Path> path) {
      return setExportDir(path, NO_OP);
    }

    public Builder setExportDir(Optional<Path> path, Consumer<String> onInvalid) {
      Optional<String> error = path.flatMap(Builder::checkExportDirInvariants);
      if (error.isPresent()) {
        onInvalid.accept(error.get());
        log.warn("Invalid export dir: {}", error.get());
      } else
        exportDir = path.orElse(null);
      return this;
    }

    private static Optional<String> checkExportDirInvariants(Path path) {
      if (!exists(path))
        return Optional.of("Given export directory doesn't exist");
      if (!isDirectory(path))
        return Optional.of("Given export directory is not a directory");
      if (isUnderODKFolder(path.toFile()))
        return Optional.of("Given export directory is inside a Collect storage directory");
      if (isUnderBriefcaseFolder(path.toFile()))
        return Optional.of("Given export directory is inside a Briefcase storage directory");
      return Optional.empty();
    }

    public Builder setPemFile(Path path) {
      return setPemFile(Optional.ofNullable(path), NO_OP);
    }

    public Builder setPemFile(Optional<Path> path) {
      return setPemFile(path, NO_OP);
    }

    public Builder setPemFile(Optional<Path> path, Consumer<String> onInvalid) {
      Optional<String> error = path.flatMap(Builder::checkPemFileInvariants);
      if (error.isPresent()) {
        onInvalid.accept(error.get());
        log.warn("Invalid PEM file: {}", error.get());
      } else
        pemFile = path.orElse(null);
      return this;
    }

    private static Optional<String> checkPemFileInvariants(Path path) {
      if (!exists(path))
        return Optional.of("Given PEM file doesn't exist");
      if (!isRegularFile(path))
        return Optional.of("Given PEM file is not a file");
      if (readPemFile(path).isEmpty())
        return Optional.of("Given PEM file can't be parsed");
      return Optional.empty();
    }

    public Builder setDateRange(DateRange dateRange) {
      this.dateRange = dateRange;
      return this;
    }

    Builder setDateRange(Optional<LocalDate> start, Optional<LocalDate> end) {
      this.dateRange = new DateRange(start, end);
      return this;
    }

    public Builder setStartDate(LocalDate date) {
      dateRange = dateRange.setStart(date);
      return this;
    }

    public Builder setEndDate(LocalDate date) {
      dateRange = dateRange.setEnd(date);
      return this;
    }

    public Builder setPullBefore(OverridableBoolean pullBefore) {
      this.pullBefore = pullBefore;
      return this;
    }

    public Builder setPullBefore(boolean value) {
      pullBefore = pullBefore.set(value);
      return this;
    }

    public Builder setOverwriteFiles(OverridableBoolean overwriteFiles) {
      this.overwriteFiles = overwriteFiles;
      return this;
    }

    public Builder setOverwriteFiles(boolean value) {
      overwriteFiles = overwriteFiles.set(value);
      return this;
    }

    public Builder setExportMedia(OverridableBoolean exportMedia) {
      this.exportMedia = exportMedia;
      return this;
    }

    public Builder setExportMedia(boolean value) {
      exportMedia = exportMedia.set(value);
      return this;
    }

    public Builder setSplitSelectMultiples(OverridableBoolean splitSelectMultiples) {
      this.splitSelectMultiples = splitSelectMultiples;
      return this;
    }

    public Builder setSplitSelectMultiples(boolean value) {
      splitSelectMultiples = splitSelectMultiples.set(value);
      return this;
    }

    public Builder setIncludeGeoJsonExport(OverridableBoolean includeGeoJsonExport) {
      this.includeGeoJsonExport = includeGeoJsonExport;
      return this;
    }

    public Builder setIncludeGeoJsonExport(boolean value) {
      includeGeoJsonExport = includeGeoJsonExport.set(value);
      return this;
    }

    public Builder setRemoveGroupNames(OverridableBoolean removeGroupNames) {
      this.removeGroupNames = removeGroupNames;
      return this;
    }

    public Builder setRemoveGroupNames(boolean value) {
      removeGroupNames = removeGroupNames.set(value);
      return this;
    }

    Builder setSmartAppend(OverridableBoolean smartAppend) {
      this.smartAppend = smartAppend;
      return this;
    }

    public Builder setSmartAppend(boolean value) {
      this.smartAppend = smartAppend.set(value);
      return this;
    }

    public Builder overridePullBefore(TriStateBoolean overrideValue) {
      pullBefore = pullBefore.overrideWith(overrideValue);
      return this;
    }

    Builder overrideOverwriteFiles(TriStateBoolean overrideValue) {
      overwriteFiles = overwriteFiles.overrideWith(overrideValue);
      return this;
    }

    Builder overrideExportMedia(TriStateBoolean overrideValue) {
      exportMedia = exportMedia.overrideWith(overrideValue);
      return this;
    }

    Builder overrideSplitSelectMultiples(TriStateBoolean overrideValue) {
      splitSelectMultiples = splitSelectMultiples.overrideWith(overrideValue);
      return this;
    }

    Builder overrideIncludeGeoJsonExport(TriStateBoolean overrideValue) {
      includeGeoJsonExport = includeGeoJsonExport.overrideWith(overrideValue);
      return this;
    }

    Builder overrideRemoveGroupNames(TriStateBoolean overrideValue) {
      removeGroupNames = removeGroupNames.overrideWith(overrideValue);
      return this;
    }

    public Builder overrideSmartAppend(TriStateBoolean overrideValue) {
      smartAppend = smartAppend.overrideWith(overrideValue);
      return this;
    }
  }
}
