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

import static org.opendatakit.briefcase.export.PullBeforeOverrideOption.INHERIT;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_PEM_FILE;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.opendatakit.briefcase.model.BriefcasePreferences;

public class ExportConfiguration {
  private static final String EXPORT_DIR = "exportDir";
  private static final String PEM_FILE = "pemFile";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String PULL_BEFORE = "pullBefore";
  private static final String FORM_NEEDS_PRIVATE_KEY = "formNeedsPrivateKey";
  private static final String PULL_BEFORE_OVERRIDE = "pullBeforeOverride";
  private static final Predicate<PullBeforeOverrideOption> ALL_EXCEPT_INHERIT = value -> value != INHERIT;
  private Optional<Path> exportDir;
  private Optional<Path> pemFile;
  private Optional<LocalDate> startDate;
  private Optional<LocalDate> endDate;
  private Optional<Boolean> pullBefore;
  private Optional<Boolean> formNeedsPrivateKey;
  private Optional<PullBeforeOverrideOption> pullBeforeOverride;

  public ExportConfiguration(Optional<Path> exportDir, Optional<Path> pemFile, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Boolean> pullBefore, Optional<PullBeforeOverrideOption> pullBeforeOverride,Optional<Boolean> formNeedsPrivateKey) {
    this.exportDir = exportDir;
    this.pemFile = pemFile;
    this.startDate = startDate;
    this.endDate = endDate;
    this.pullBefore = pullBefore;
    this.pullBeforeOverride = pullBeforeOverride;
    this.formNeedsPrivateKey = formNeedsPrivateKey;
  }

  public static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static ExportConfiguration load(BriefcasePreferences prefs) {
    return new ExportConfiguration(
        prefs.nullSafeGet(EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(END_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(PULL_BEFORE).map(Boolean::valueOf),
        prefs.nullSafeGet(PULL_BEFORE_OVERRIDE).map(PullBeforeOverrideOption::from),
        prefs.nullSafeGet(FORM_NEEDS_PRIVATE_KEY).map(Boolean::valueOf)
    );
  }

  public static ExportConfiguration load(BriefcasePreferences prefs, String keyPrefix) {
    return new ExportConfiguration(
        prefs.nullSafeGet(keyPrefix + EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + END_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + PULL_BEFORE).map(Boolean::valueOf),
        prefs.nullSafeGet(keyPrefix + PULL_BEFORE_OVERRIDE).map(PullBeforeOverrideOption::from),
        prefs.nullSafeGet(keyPrefix + FORM_NEEDS_PRIVATE_KEY).map(Boolean::valueOf)
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
        keyPrefix + FORM_NEEDS_PRIVATE_KEY
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
    formNeedsPrivateKey.ifPresent(value -> map.put(keyPrefix + FORM_NEEDS_PRIVATE_KEY, value.toString()));
    return map;
  }

  public ExportConfiguration copy() {
    return new ExportConfiguration(
        exportDir,
        pemFile,
        startDate,
        endDate,
        pullBefore,
        pullBeforeOverride,
            formNeedsPrivateKey
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
    if (pemFile.isPresent())  {
      Path path = pemFile.get();
      //Check if path is a file and is readable
      if (Files.isRegularFile(path) && Files.isReadable(path)) {
        //todo check if file is parsable by PEMParser and contains private key
        return true;
      }
    }
    return false;
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

  public Optional<Boolean> getFormNeedsPrivateKey() {
    return formNeedsPrivateKey;
  }

  public ExportConfiguration setFormNeedsPrivateKey(Boolean value) {
    this.formNeedsPrivateKey = Optional.ofNullable(value);
    return this;
  }

  public Optional<PullBeforeOverrideOption> getPullBeforeOverride() {
    return pullBeforeOverride;
  }

  public ExportConfiguration setPullBeforeOverride(PullBeforeOverrideOption value) {
    this.pullBeforeOverride = Optional.ofNullable(value);
    return this;
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
    return Stream.of(
        pullBeforeOverride.filter(ALL_EXCEPT_INHERIT).flatMap(PullBeforeOverrideOption::asBoolean),
        pullBefore
    ).filter(Optional::isPresent).map(Optional::get).findFirst().orElse(false);
  }

  private boolean isDateRangeValid() {
    return !startDate.isPresent() || !endDate.isPresent() || startDate.get().isBefore(endDate.get());
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

  public void ifPullBeforeOverridePresent(Consumer<PullBeforeOverrideOption> consumer) {
    pullBeforeOverride.ifPresent(consumer);
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
    if (formNeedsPrivateKey.isPresent() && !isPemFilePresent())
      errors.add(INVALID_PEM_FILE);
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
    if (formNeedsPrivateKey.isPresent() && !isPemFilePresent())
      errors.add(INVALID_PEM_FILE);

    return errors;
  }

  public boolean isEmpty() {
    return !exportDir.isPresent()
        && !pemFile.isPresent()
        && !startDate.isPresent()
        && !endDate.isPresent()
        && !pullBefore.isPresent()
        && !pullBeforeOverride.filter(ALL_EXCEPT_INHERIT).isPresent();
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
        exportDir.isPresent() ? exportDir : defaultConfiguration.exportDir,
        pemFile.isPresent() ? pemFile : defaultConfiguration.pemFile,
        startDate.isPresent() ? startDate : defaultConfiguration.startDate,
        endDate.isPresent() ? endDate : defaultConfiguration.endDate,
        pullBefore.isPresent() ? pullBefore : defaultConfiguration.pullBefore,
        pullBeforeOverride.isPresent() ? pullBeforeOverride : defaultConfiguration.pullBeforeOverride,
        formNeedsPrivateKey.isPresent() ? formNeedsPrivateKey : defaultConfiguration.formNeedsPrivateKey
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
        Objects.equals(pullBeforeOverride, that.pullBeforeOverride);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exportDir, pemFile, startDate, endDate, pullBefore, pullBeforeOverride);
  }
}
