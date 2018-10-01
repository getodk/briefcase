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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bouncycastle.openssl.PEMReader;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.reused.TriStateBoolean;
import org.opendatakit.briefcase.util.ErrorOr;

public class ExportConfiguration {
  private static final String EXPORT_DIR = "exportDir";
  private static final String PEM_FILE = "pemFile";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String PULL_BEFORE = "pullBefore";
  private static final String PULL_BEFORE_OVERRIDE = "pullBeforeOverride";
  private static final String OVERWRITE_FILES = "overwriteExistingFiles";
  private static final String OVERWRITE_FILES_OVERRIDE = "overwriteFilesOverride";
  private static final String EXPORT_MEDIA = "exportMedia";
  private static final String EXPORT_MEDIA_OVERRIDE = "exportMediaOverride";
  private static final String EXPLODE_CHOICE_LISTS = "explodeChoiceLists";
  private Optional<String> exportFileName;
  private Optional<Path> exportDir;
  private Optional<Path> pemFile;
  private Optional<LocalDate> startDate;
  private Optional<LocalDate> endDate;
  public OverridableBoolean pullBefore;
  public OverridableBoolean overwriteFiles;
  public OverridableBoolean exportMedia;
  private Optional<Boolean> explodeChoiceLists;

  public ExportConfiguration(Optional<String> exportFileName, Optional<Path> exportDir, Optional<Path> pemFile, Optional<LocalDate> startDate, Optional<LocalDate> endDate, OverridableBoolean pullBefore, OverridableBoolean overwriteFiles, OverridableBoolean exportMedia, Optional<Boolean> explodeChoiceLists) {
    this.exportFileName = exportFileName;
    this.exportDir = exportDir;
    this.pemFile = pemFile;
    this.startDate = startDate;
    this.endDate = endDate;
    this.pullBefore = pullBefore;
    this.overwriteFiles = overwriteFiles;
    this.exportMedia = exportMedia;
    this.explodeChoiceLists = explodeChoiceLists;
  }

  public static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), OverridableBoolean.empty(), OverridableBoolean.empty(), OverridableBoolean.empty(), Optional.empty());
  }

  public static ExportConfiguration load(BriefcasePreferences prefs) {
    return load(prefs, "");
  }

  public static ExportConfiguration load(BriefcasePreferences prefs, String keyPrefix) {
    return new ExportConfiguration(
        Optional.empty(),
        prefs.nullSafeGet(keyPrefix + EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + END_DATE).map(LocalDate::parse),
        readOverridableBoolean(prefs, keyPrefix + PULL_BEFORE, keyPrefix + PULL_BEFORE_OVERRIDE),
        readOverridableBoolean(prefs, keyPrefix + OVERWRITE_FILES, keyPrefix + OVERWRITE_FILES_OVERRIDE),
        readOverridableBoolean(prefs, keyPrefix + EXPORT_MEDIA, keyPrefix + EXPORT_MEDIA_OVERRIDE),
        prefs.nullSafeGet(keyPrefix + EXPLODE_CHOICE_LISTS).map(Boolean::valueOf)
    );
  }

  public static OverridableBoolean readOverridableBoolean(BriefcasePreferences prefs, String mainKey, String overrideKey) {
    OverridableBoolean ob = prefs.nullSafeGet(mainKey).map(OverridableBoolean::from).orElseGet(OverridableBoolean::empty);
    prefs.nullSafeGet(overrideKey).map(TriStateBoolean::from).ifPresent(ob::overrideWith);
    return ob;
  }

  public static List<String> keys() {
    return keys("");
  }

  public static List<String> keys(String keyPrefix) {
    return Arrays.asList(
        keyPrefix + EXPORT_DIR,
        keyPrefix + PEM_FILE,
        keyPrefix + START_DATE,
        keyPrefix + END_DATE,
        keyPrefix + PULL_BEFORE,
        keyPrefix + OVERWRITE_FILES,
        keyPrefix + EXPORT_MEDIA,
        keyPrefix + EXPLODE_CHOICE_LISTS
    );
  }

  public Map<String, String> asMap() {
    return asMap("");
  }

  public Map<String, String> asMap(String keyPrefix) {
    // This should be a stream of tuples that's reduces into a
    // map but we'll have to wait for that
    HashMap<String, String> map = new HashMap<>();
    exportDir.ifPresent(value -> map.put(keyPrefix + EXPORT_DIR, value.toString()));
    pemFile.ifPresent(value -> map.put(keyPrefix + PEM_FILE, value.toString()));
    startDate.ifPresent(value -> map.put(keyPrefix + START_DATE, value.format(DateTimeFormatter.ISO_DATE)));
    endDate.ifPresent(value -> map.put(keyPrefix + END_DATE, value.format(DateTimeFormatter.ISO_DATE)));
    map.put(keyPrefix + PULL_BEFORE, pullBefore.serialize());
    map.put(keyPrefix + OVERWRITE_FILES, overwriteFiles.serialize());
    map.put(keyPrefix + EXPORT_MEDIA, exportMedia.serialize());
    explodeChoiceLists.ifPresent(value -> map.put(keyPrefix + EXPLODE_CHOICE_LISTS, value.toString()));
    return map;
  }

  public ExportConfiguration copy() {
    return new ExportConfiguration(
        exportFileName, exportDir,
        pemFile,
        startDate,
        endDate,
        pullBefore,
        overwriteFiles,
        exportMedia,
        explodeChoiceLists
    );
  }

  public Path getExportDir() {
    return exportDir.orElseThrow(BriefcaseException::new);
  }

  public ExportConfiguration setExportDir(Path path) {
    this.exportDir = Optional.ofNullable(path);
    return this;
  }

  public Path getPemFile() {
    return pemFile.orElseThrow(BriefcaseException::new);
  }

  public ExportConfiguration setPemFile(Path path) {
    this.pemFile = Optional.ofNullable(path);
    return this;
  }

  public boolean isPemFilePresent() {
    return pemFile.isPresent();
  }

  public ExportConfiguration setStartDate(LocalDate date) {
    this.startDate = Optional.ofNullable(date);
    return this;
  }

  public ExportConfiguration setEndDate(LocalDate date) {
    this.endDate = Optional.ofNullable(date);
    return this;
  }

  boolean resolveExplodeChoiceLists() {
    return explodeChoiceLists.orElse(false);
  }

  /**
   * Resolves if we need to pull forms depending on the pullBefore and pullBeforeOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the pullBeforeOverride Optional holds a {@link TriStateBoolean} value
   * different than {@link TriStateBoolean#UNDETERMINED}, then it returns its associated
   * boolean value</li>
   * <li>if the pullBefore Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return true if the algorithm resolves that we need to pull forms, false otherwise
   */
  public boolean resolvePullBefore() {
    return pullBefore.resolve(false);
  }

  /**
   * Resolves if we need to export media files depending on the exportMedia and exportMediaOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the exportMediaOverride Optional holds an {@link TriStateBoolean} value
   * different than {@link TriStateBoolean#UNDETERMINED}, then it returns its associated
   * boolean value</li>
   * <li>if the exportMedia Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return false if the algorithm resolves that we don't need to export media files, true otherwise
   */
  public boolean resolveExportMedia() {
    return exportMedia.resolve(true);
  }

  /**
   * Resolves if we need to overwrite files depending on the overwriteExistingFiles and overwriteFilesOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the overwriteFilesOverride Optional holds an {@link TriStateBoolean} value
   * different than {@link TriStateBoolean#UNDETERMINED}, then it returns its associated
   * boolean value</li>
   * <li>if the overwriteExistingFiles Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return false if the algorithm resolves that we don't need to overwrite files, true otherwise
   */
  boolean resolveOverwriteExistingFiles() {
    return overwriteFiles.resolve(false);
  }

  private boolean isDateRangeValid() {
    return !startDate.isPresent() || !endDate.isPresent() || !startDate.get().isAfter(endDate.get());
  }

  public void ifExportDirPresent(Consumer<Path> consumer) {
    exportDir.ifPresent(consumer);
  }

  public void ifPemFilePresent(Consumer<Path> consumer) {
    pemFile.ifPresent(consumer);
  }

  public void ifStartDatePresent(Consumer<LocalDate> consumer) {
    startDate.ifPresent(consumer);
  }

  public void ifEndDatePresent(Consumer<LocalDate> consumer) {
    endDate.ifPresent(consumer);
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (!exportDir.isPresent())
      errors.add("Export directory was not specified.");

    if (!exportDir.filter(path -> exists(path)).isPresent())
      errors.add(DIR_NOT_EXIST);

    if (!exportDir.filter(path -> isDirectory(path)).isPresent())
      errors.add(DIR_NOT_DIRECTORY);

    if (!exportDir.filter(path -> !isUnderODKFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_ODK_DEVICE_DIRECTORY);

    if (!exportDir.filter(path -> !isUnderBriefcaseFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_BRIEFCASE_STORAGE);

    if (startDate.isPresent() && !endDate.isPresent())
      errors.add("Missing date range end definition");
    if (!startDate.isPresent() && endDate.isPresent())
      errors.add("Missing date range start definition");
    if (!isDateRangeValid())
      errors.add(INVALID_DATE_RANGE_MESSAGE);
    return errors;
  }

  private List<String> getCustomConfErrors() {
    List<String> errors = new ArrayList<>();

    if (exportDir.isPresent() && !exportDir.filter(path -> exists(path)).isPresent())
      errors.add(DIR_NOT_EXIST);

    if (exportDir.isPresent() && !exportDir.filter(path -> isDirectory(path)).isPresent())
      errors.add(DIR_NOT_DIRECTORY);

    if (exportDir.isPresent() && !exportDir.filter(path -> !isUnderODKFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_ODK_DEVICE_DIRECTORY);

    if (exportDir.isPresent() && !exportDir.filter(path -> !isUnderBriefcaseFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_BRIEFCASE_STORAGE);

    if (startDate.isPresent() && !endDate.isPresent())
      errors.add("Missing date range end definition");
    if (!startDate.isPresent() && endDate.isPresent())
      errors.add("Missing date range start definition");
    if (!isDateRangeValid())
      errors.add(INVALID_DATE_RANGE_MESSAGE);

    return errors;
  }

  public boolean isEmpty() {
    return !exportDir.isPresent()
        && !pemFile.isPresent()
        && !startDate.isPresent()
        && !endDate.isPresent()
        && pullBefore.isEmpty()
        && overwriteFiles.isEmpty()
        && exportMedia.isEmpty()
        && !explodeChoiceLists.isPresent();
  }

  public boolean isValid() {
    return getErrors().isEmpty();
  }

  public boolean isValidAsCustomConf() {
    return getCustomConfErrors().isEmpty();
  }

  public <T> Optional<T> mapPemFile(Function<Path, T> mapper) {
    return pemFile.map(mapper);
  }

  public <T> Optional<T> mapExportDir(Function<Path, T> mapper) {
    return exportDir.map(mapper);
  }

  public <T> Optional<T> mapStartDate(Function<LocalDate, T> mapper) {
    return startDate.map(mapper);
  }

  public <T> Optional<T> mapEndDate(Function<LocalDate, T> mapper) {
    return endDate.map(mapper);
  }

  public ExportConfiguration fallingBackTo(ExportConfiguration defaultConfiguration) {
    return new ExportConfiguration(
        exportFileName, exportDir.isPresent() ? exportDir : defaultConfiguration.exportDir,
        pemFile.isPresent() ? pemFile : defaultConfiguration.pemFile,
        startDate.isPresent() ? startDate : defaultConfiguration.startDate,
        endDate.isPresent() ? endDate : defaultConfiguration.endDate,
        pullBefore.fallingBackTo(defaultConfiguration.pullBefore),
        overwriteFiles.fallingBackTo(defaultConfiguration.overwriteFiles),
        exportMedia.fallingBackTo(defaultConfiguration.exportMedia),
        explodeChoiceLists.isPresent() ? explodeChoiceLists : defaultConfiguration.explodeChoiceLists
    );
  }

  @Override
  public String toString() {
    return "ExportConfiguration{" +
        "exportDir=" + exportDir +
        ", pemFile=" + pemFile +
        ", startDate=" + startDate +
        ", endDate=" + endDate +
        ", pullBefore=" + pullBefore +
        ", overwriteFiles=" + overwriteFiles +
        ", exportMedia=" + exportMedia +
        ", explodeChoiceLists=" + explodeChoiceLists +
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
        Objects.equals(startDate, that.startDate) &&
        Objects.equals(endDate, that.endDate) &&
        Objects.equals(pullBefore, that.pullBefore) &&
        Objects.equals(overwriteFiles, that.overwriteFiles) &&
        Objects.equals(exportMedia, that.exportMedia) &&
        Objects.equals(explodeChoiceLists, that.explodeChoiceLists);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exportDir, pemFile, startDate, endDate, pullBefore, overwriteFiles, exportMedia, explodeChoiceLists);
  }

  public static ErrorOr<PrivateKey> readPemFile(Path pemFile) {
    try (InputStream is = newInputStream(pemFile);
         InputStreamReader isr = new InputStreamReader(is, UTF_8);
         BufferedReader br = new BufferedReader(isr);
         PEMReader pr = new PEMReader(br)
    ) {
      Object o = pr.readObject();
      if (o == null)
        return ErrorOr.error("The supplied file is not in PEM format");

      if (o instanceof KeyPair)
        return ErrorOr.some(((KeyPair) o).getPrivate());

      if (o instanceof PrivateKey)
        return ErrorOr.some(((PrivateKey) o));

      return ErrorOr.error("The supplied file does not contain a private key");
    } catch (IOException e) {
      return ErrorOr.error("Briefcase can't read the provided file: " + e.getMessage());
    }
  }

  public Optional<PrivateKey> getPrivateKey() {
    return pemFile.map(ExportConfiguration::readPemFile).flatMap(ErrorOr::asOptional);
  }

  public DateRange getDateRange() {
    return new DateRange(startDate, endDate);
  }

  public Path getExportMediaPath() {
    return exportDir.map(dir -> dir.resolve("media"))
        .orElseThrow(() -> new BriefcaseException("No export dir configured"));
  }

  public Optional<String> getExportFileName() {
    return exportFileName.map(filename -> filename.toLowerCase().endsWith(".csv") ? filename : filename + ".csv");
  }

  public Path getErrorsDir(String formName) {
    return exportDir.map(dir -> dir.resolve(stripIllegalChars(formName) + " - errors")).orElseThrow(BriefcaseException::new);
  }
}
