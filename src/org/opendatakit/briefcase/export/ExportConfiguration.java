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

import static org.opendatakit.briefcase.reused.OptionalProduct.firstPresent;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
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
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.bouncycastle.openssl.PEMReader;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.ui.export.components.CustomConfBoolean;
import org.opendatakit.briefcase.util.ErrorsOr;

public class ExportConfiguration {
  private static final String EXPORT_DIR = "exportDir";
  private static final String PEM_FILE = "pemFile";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String PULL_BEFORE = "pullBefore";
  private static final String PULL_BEFORE_OVERRIDE = "pullBeforeOverride";
  private static final String OVERWRITE_EXISTING_FILES = "overwriteExistingFiles";
  private static final String OVERWRITE_FILES_OVERRIDE = "overwriteFilesOverride";
  private static final String EXPORT_MEDIA = "exportMedia";
  private static final String EXPORT_MEDIA_OVERRIDE = "exportMediaOverride";
  private static final String EXPLODE_CHOICE_LISTS = "explodeChoiceLists";
  private static final Predicate<PullBeforeOverrideOption> PULL_BEFORE_ALL_EXCEPT_INHERIT = value -> value != PullBeforeOverrideOption.INHERIT;
  private static final Predicate<ExportMediaOverrideOption> EXPORT_MEDIA_ALL_EXCEPT_INHERIT = value -> value != ExportMediaOverrideOption.INHERIT;
  private static final Predicate<CustomConfBoolean.Value> ALL_EXCEPT_INHERIT = value -> value != CustomConfBoolean.Value.INHERIT;
  private Optional<String> exportFileName;
  private Optional<Path> exportDir;
  private Optional<Path> pemFile;
  private Optional<LocalDate> startDate;
  private Optional<LocalDate> endDate;
  private Optional<Boolean> pullBefore;
  private Optional<CustomConfBoolean.Value> pullBeforeOverride;
  private Optional<Boolean> overwriteExistingFiles;
  private Optional<CustomConfBoolean.Value> overwriteFilesOverride;
  private Optional<Boolean> exportMedia;
  private Optional<CustomConfBoolean.Value> exportMediaOverride;
  private Optional<Boolean> explodeChoiceLists;

  public ExportConfiguration(Optional<String> exportFileName, Optional<Path> exportDir, Optional<Path> pemFile, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Boolean> pullBefore, Optional<CustomConfBoolean.Value> pullBeforeOverride, Optional<Boolean> overwriteExistingFiles, Optional<CustomConfBoolean.Value> overwriteFilesOverride, Optional<Boolean> exportMedia, Optional<CustomConfBoolean.Value> exportMediaOverride, Optional<Boolean> explodeChoiceLists) {
    this.exportFileName = exportFileName;
    this.exportDir = exportDir;
    this.pemFile = pemFile;
    this.startDate = startDate;
    this.endDate = endDate;
    this.pullBefore = pullBefore;
    this.pullBeforeOverride = pullBeforeOverride;
    this.overwriteExistingFiles = overwriteExistingFiles;
    this.overwriteFilesOverride = overwriteFilesOverride;
    this.exportMedia = exportMedia;
    this.exportMediaOverride = exportMediaOverride;
    this.explodeChoiceLists = explodeChoiceLists;
  }

  public static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static ExportConfiguration load(BriefcasePreferences prefs) {
    return new ExportConfiguration(
        Optional.empty(),
        prefs.nullSafeGet(EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(END_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(PULL_BEFORE).map(Boolean::valueOf),
        prefs.nullSafeGet(PULL_BEFORE_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(OVERWRITE_EXISTING_FILES).map(Boolean::valueOf),
        prefs.nullSafeGet(OVERWRITE_FILES_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(EXPORT_MEDIA).map(Boolean::valueOf),
        prefs.nullSafeGet(EXPORT_MEDIA_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(EXPLODE_CHOICE_LISTS).map(Boolean::valueOf)
    );
  }

  public static ExportConfiguration load(BriefcasePreferences prefs, String keyPrefix) {
    return new ExportConfiguration(
        Optional.empty(),
        prefs.nullSafeGet(keyPrefix + EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + END_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + PULL_BEFORE).map(Boolean::valueOf),
        prefs.nullSafeGet(keyPrefix + PULL_BEFORE_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(keyPrefix + OVERWRITE_EXISTING_FILES).map(Boolean::valueOf),
        prefs.nullSafeGet(keyPrefix + OVERWRITE_FILES_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(keyPrefix + EXPORT_MEDIA).map(Boolean::valueOf),
        prefs.nullSafeGet(keyPrefix + EXPORT_MEDIA_OVERRIDE).map(CustomConfBoolean.Value::valueOf),
        prefs.nullSafeGet(keyPrefix + EXPLODE_CHOICE_LISTS).map(Boolean::valueOf)
    );
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
        keyPrefix + PULL_BEFORE_OVERRIDE,
        keyPrefix + OVERWRITE_EXISTING_FILES,
        keyPrefix + OVERWRITE_FILES_OVERRIDE,
        keyPrefix + EXPORT_MEDIA,
        keyPrefix + EXPORT_MEDIA_OVERRIDE,
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
    pullBefore.ifPresent(value -> map.put(keyPrefix + PULL_BEFORE, value.toString()));
    pullBeforeOverride.filter(ALL_EXCEPT_INHERIT).ifPresent(value -> map.put(keyPrefix + PULL_BEFORE_OVERRIDE, value.name()));
    overwriteExistingFiles.ifPresent(value -> map.put(keyPrefix + OVERWRITE_EXISTING_FILES, value.toString()));
    overwriteFilesOverride.filter(ALL_EXCEPT_INHERIT).ifPresent(value -> map.put(keyPrefix + OVERWRITE_FILES_OVERRIDE, value.name()));
    exportMedia.ifPresent(value -> map.put(keyPrefix + EXPORT_MEDIA, value.toString()));
    exportMediaOverride.filter(ALL_EXCEPT_INHERIT).ifPresent(value -> map.put(keyPrefix + EXPORT_MEDIA_OVERRIDE, value.name()));
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
        pullBeforeOverride,
        overwriteExistingFiles,
        overwriteFilesOverride,
        exportMedia,
        exportMediaOverride,
        explodeChoiceLists
    );
  }

  public Optional<Path> getExportDir() {
    return exportDir;
  }

  public ExportConfiguration setExportDir(Path path) {
    this.exportDir = Optional.ofNullable(path);
    return this;
  }

  public Optional<Path> getPemFile() {
    return pemFile;
  }

  public ExportConfiguration setPemFile(Path path) {
    this.pemFile = Optional.ofNullable(path);
    return this;
  }

  public boolean isPemFilePresent() {
    return pemFile.isPresent();
  }

  public Optional<LocalDate> getStartDate() {
    return startDate;
  }

  public ExportConfiguration setStartDate(LocalDate date) {
    this.startDate = Optional.ofNullable(date);
    return this;
  }

  public Optional<LocalDate> getEndDate() {
    return endDate;
  }

  public ExportConfiguration setEndDate(LocalDate date) {
    this.endDate = Optional.ofNullable(date);
    return this;
  }

  public Optional<Boolean> getPullBefore() {
    return pullBefore;
  }

  public ExportConfiguration setPullBefore(Boolean value) {
    this.pullBefore = Optional.ofNullable(value);
    return this;
  }

  public Optional<CustomConfBoolean.Value> getPullBeforeOverride() {
    return pullBeforeOverride;
  }

  public ExportConfiguration setPullBeforeOverride(CustomConfBoolean.Value value) {
    this.pullBeforeOverride = Optional.ofNullable(value);
    return this;
  }

  public ExportConfiguration setOverwriteExistingFiles(Boolean value) {
    this.overwriteExistingFiles = Optional.ofNullable(value);
    return this;
  }

  public ExportConfiguration setOverwriteFilesOverride(CustomConfBoolean.Value value) {
    this.overwriteFilesOverride = Optional.of(value);
    return this;
  }

  public ExportConfiguration setExportMedia(boolean exportMedia) {
    this.exportMedia = Optional.of(exportMedia);
    return this;
  }

  public ExportConfiguration setExportMediaOverride(CustomConfBoolean.Value value) {
    this.exportMediaOverride = Optional.of(value);
    return this;
  }

  boolean resolveExplodeChoiceLists() {
    return explodeChoiceLists.orElse(false);
  }

  /**
   * Resolves if we need to pull forms depending on the pullBefore and pullBeforeOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the pullBeforeOverride Optional holds a {@link PullBeforeOverrideOption} value
   * different than {@link PullBeforeOverrideOption#INHERIT}, then it returns its associated
   * boolean value</li>
   * <li>if the pullBefore Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return true if the algorithm resolves that we need to pull forms, false otherwise
   */
  public boolean resolvePullBefore() {
    return firstPresent(
        pullBeforeOverride.filter(ALL_EXCEPT_INHERIT).flatMap(CustomConfBoolean.Value::getBooleanValue),
        pullBefore
    ).orElse(false);
  }

  /**
   * Resolves if we need to export media files depending on the exportMedia and exportMediaOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the exportMediaOverride Optional holds an {@link ExportMediaOverrideOption} value
   * different than {@link ExportMediaOverrideOption#INHERIT}, then it returns its associated
   * boolean value</li>
   * <li>if the exportMedia Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return false if the algorithm resolves that we don't need to export media files, true otherwise
   */
  public boolean resolveExportMedia() {
    return firstPresent(
        exportMediaOverride.filter(ALL_EXCEPT_INHERIT).flatMap(CustomConfBoolean.Value::getBooleanValue),
        exportMedia
    ).orElse(true);
  }

  /**
   * Resolves if we need to overwrite files depending on the overwriteExistingFiles and overwriteFilesOverride
   * settings with the following algorithm:
   * <ul>
   * <li>if the overwriteFilesOverride Optional holds an {@link CustomConfBoolean.Value} value
   * different than {@link CustomConfBoolean.Value#INHERIT}, then it returns its associated
   * boolean value</li>
   * <li>if the overwriteExistingFiles Optional holds a Boolean value, then it returns it.</li>
   * <li>otherwise, it returns false</li>
   * </ul>
   * See the tests on ExportConfigurationTests to see all the specific cases.
   *
   * @return false if the algorithm resolves that we don't need to overwrite files, true otherwise
   */
  boolean resolveOverwriteExistingFiles() {
    return firstPresent(
        overwriteFilesOverride.filter(ALL_EXCEPT_INHERIT).flatMap(CustomConfBoolean.Value::getBooleanValue),
        overwriteExistingFiles
    ).orElse(true);
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

  public void ifPullBeforePresent(Consumer<Boolean> consumer) {
    pullBefore.ifPresent(consumer);
  }

  public void ifPullBeforeOverridePresent(Consumer<CustomConfBoolean.Value> consumer) {
    pullBeforeOverride.ifPresent(consumer);
  }

  public void ifOverwriteExistingFilesPresent(Consumer<Boolean> consumer) {
    overwriteExistingFiles.ifPresent(consumer);
  }

  public void ifOverwriteFilesOverridePresent(Consumer<CustomConfBoolean.Value> consumer) {
    overwriteFilesOverride.ifPresent(consumer);
  }

  public void ifExportMediaOverridePresent(Consumer<CustomConfBoolean.Value> consumer) {
    exportMediaOverride.ifPresent(consumer);
  }

  public void isExplodeChoiceListPresent(Consumer<Boolean> consumer) {
    explodeChoiceLists.ifPresent(consumer);
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (!exportDir.isPresent())
      errors.add("Export directory was not specified.");

    if (!exportDir.filter(path -> Files.exists(path)).isPresent())
      errors.add(DIR_NOT_EXIST);

    if (!exportDir.filter(path -> Files.isDirectory(path)).isPresent())
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

    if (exportDir.isPresent() && !exportDir.filter(path -> Files.exists(path)).isPresent())
      errors.add(DIR_NOT_EXIST);

    if (exportDir.isPresent() && !exportDir.filter(path -> Files.isDirectory(path)).isPresent())
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
        && !pullBefore.isPresent()
        && !pullBeforeOverride.filter(ALL_EXCEPT_INHERIT).isPresent()
        && !overwriteExistingFiles.isPresent()
        && !overwriteFilesOverride.filter(ALL_EXCEPT_INHERIT).isPresent()
        && !exportMedia.isPresent()
        && !exportMediaOverride.filter(ALL_EXCEPT_INHERIT).isPresent()
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
        pullBefore.isPresent() ? pullBefore : defaultConfiguration.pullBefore,
        pullBeforeOverride.isPresent() ? pullBeforeOverride : defaultConfiguration.pullBeforeOverride,
        overwriteExistingFiles.isPresent() ? overwriteExistingFiles : defaultConfiguration.overwriteExistingFiles,
        overwriteFilesOverride.isPresent() ? overwriteFilesOverride : defaultConfiguration.overwriteFilesOverride,
        exportMedia.isPresent() ? exportMedia : defaultConfiguration.exportMedia,
        exportMediaOverride.isPresent() ? exportMediaOverride : defaultConfiguration.exportMediaOverride,
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
        ", pullBeforeOverride=" + pullBeforeOverride +
        ", overwriteExistingFiles=" + overwriteExistingFiles +
        ", overwriteFilesOverride=" + overwriteFilesOverride +
        ", exportMedia=" + exportMedia +
        ", exportMediaOverride=" + exportMediaOverride +
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
        Objects.equals(pullBeforeOverride, that.pullBeforeOverride) &&
        Objects.equals(overwriteExistingFiles, that.overwriteExistingFiles) &&
        Objects.equals(overwriteFilesOverride, that.overwriteFilesOverride) &&
        Objects.equals(exportMedia, that.exportMedia) &&
        Objects.equals(exportMediaOverride, that.exportMediaOverride) &&
        Objects.equals(explodeChoiceLists, that.explodeChoiceLists);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exportDir, pemFile, startDate, endDate, pullBefore, pullBeforeOverride, overwriteExistingFiles, overwriteFilesOverride, exportMedia, exportMediaOverride, explodeChoiceLists);
  }

  public static ErrorsOr<PrivateKey> readPemFile(Path pemFile) {
    try (PEMReader rdr = new PEMReader(new BufferedReader(new InputStreamReader(Files.newInputStream(pemFile), "UTF-8")))) {
      Optional<Object> o = Optional.ofNullable(rdr.readObject());
      if (!o.isPresent())
        return ErrorsOr.errors("The supplied file is not in PEM format.");
      Optional<PrivateKey> pk = extractPrivateKey(o.get());
      if (!pk.isPresent())
        return ErrorsOr.errors("The supplied file does not contain a private key.");
      return ErrorsOr.some(pk.get());
    } catch (IOException e) {
      return ErrorsOr.errors("Briefcase can't read the provided file: " + e.getMessage());
    }
  }

  private static Optional<PrivateKey> extractPrivateKey(Object o) {
    if (o instanceof KeyPair)
      return Optional.of(((KeyPair) o).getPrivate());
    if (o instanceof PrivateKey)
      return Optional.of((PrivateKey) o);
    return Optional.empty();
  }

  public Optional<PrivateKey> getPrivateKey() {
    return pemFile.map(ExportConfiguration::readPemFile).flatMap(ErrorsOr::asOptional);
  }

  public DateRange getDateRange() {
    return new DateRange(startDate, endDate);
  }

  public Path getExportMediaPath() {
    return exportDir.map(dir -> dir.resolve("media"))
        .orElseThrow(() -> new BriefcaseException("No export dir configured"));
  }

  public Optional<String> getExportFileName() {
    return exportFileName;
  }
}
