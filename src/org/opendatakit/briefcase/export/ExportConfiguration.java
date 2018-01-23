package org.opendatakit.briefcase.export;

import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.model.BriefcasePreferences;

public class ExportConfiguration {
  private static final String EXPORT_DIR = "exportDir";
  private static final String PEM_FILE = "pemFile";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private Optional<Path> exportDir;
  private Optional<Path> pemFile;
  private Optional<LocalDate> startDate;
  private Optional<LocalDate> endDate;

  private ExportConfiguration(Optional<Path> exportDir, Optional<Path> pemFile, Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    this.exportDir = exportDir;
    this.pemFile = pemFile;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static ExportConfiguration load(BriefcasePreferences prefs) {
    return new ExportConfiguration(
        prefs.nullSafeGet(EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(END_DATE).map(LocalDate::parse)
    );
  }

  public static ExportConfiguration load(BriefcasePreferences prefs, String keyPrefix) {
    return new ExportConfiguration(
        prefs.nullSafeGet(keyPrefix + EXPORT_DIR).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + PEM_FILE).map(Paths::get),
        prefs.nullSafeGet(keyPrefix + START_DATE).map(LocalDate::parse),
        prefs.nullSafeGet(keyPrefix + END_DATE).map(LocalDate::parse)
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
    return map;
  }

  public ExportConfiguration copy() {
    return new ExportConfiguration(
        exportDir,
        pemFile,
        startDate,
        endDate
    );
  }

  public Optional<Path> getExportDir() {
    return exportDir;
  }

  public Optional<Path> getPemFile() {
    return pemFile;
  }

  public Optional<LocalDate> getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate date) {
    this.startDate = Optional.ofNullable(date);
  }

  public Optional<LocalDate> getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate date) {
    this.endDate = Optional.ofNullable(date);
  }

  private boolean isDateRangeValid() {
    return !startDate.isPresent() || !endDate.isPresent() || startDate.get().isBefore(endDate.get());
  }

  public void setExportDir(Path path) {
    this.exportDir = Optional.ofNullable(path);
  }

  public void setPemFile(Path path) {
    this.pemFile = Optional.ofNullable(path);
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
      errors.add("Invalid date range: \"From\" date must be before \"To\" date.");
    return errors;
  }

  public boolean isValid() {
    return getErrors().isEmpty();
  }

  public boolean isEmpty() {
    return !exportDir.isPresent();
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

  @Override
  public String toString() {
    return "ExportConfiguration{" +
        "exportDir=" + exportDir +
        ", pemFile=" + pemFile +
        ", startDate=" + startDate +
        ", endDate=" + endDate +
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
        Objects.equals(endDate, that.endDate);
  }

  @Override
  public int hashCode() {

    return Objects.hash(exportDir, pemFile, startDate, endDate);
  }
}
