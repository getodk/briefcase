package org.opendatakit.briefcase.ui.export;

import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExportConfiguration {
  private Optional<Path> exportDirectory;
  private Optional<Path> pemFile;
  private Optional<LocalDate> dateRangeStart;
  private Optional<LocalDate> dateRangeEnd;

  private ExportConfiguration(Optional<Path> exportDirectory, Optional<Path> pemFile, Optional<LocalDate> dateRangeStart, Optional<LocalDate> dateRangeEnd) {
    this.exportDirectory = exportDirectory;
    this.pemFile = pemFile;
    this.dateRangeStart = dateRangeStart;
    this.dateRangeEnd = dateRangeEnd;
  }

  static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public void setDateRangeStart(LocalDate date) {
    this.dateRangeStart = Optional.ofNullable(date);
  }

  public void setDateRangeEnd(LocalDate date) {
    this.dateRangeEnd = Optional.ofNullable(date);
  }

  public boolean isDateRangeValid() {
    return !dateRangeStart.isPresent() || !dateRangeEnd.isPresent() || dateRangeStart.get().isBefore(dateRangeEnd.get());
  }

  public void setExportDir(Path path) {
    this.exportDirectory = Optional.ofNullable(path);
  }

  public void setPemFile(Path path) {
    this.pemFile = Optional.ofNullable(path);
  }

  public void ifExportDirPresent(Consumer<Path> consumer) {
    exportDirectory.ifPresent(consumer);
  }

  public void ifPemFilePresent(Consumer<Path> consumer) {
    pemFile.ifPresent(consumer);
  }

  public void ifDateRangeStartPresent(Consumer<LocalDate> consumer) {
    dateRangeStart.ifPresent(consumer);
  }

  public void ifDateRangeEndPresent(Consumer<LocalDate> consumer) {
    dateRangeEnd.ifPresent(consumer);
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (!exportDirectory.isPresent())
      errors.add("Export directory was not specified.");

    if (!exportDirectory.filter(path -> Files.exists(path)).isPresent())
      errors.add(DIR_NOT_EXIST);

    if (!exportDirectory.filter(path -> Files.isDirectory(path)).isPresent())
      errors.add(DIR_NOT_DIRECTORY);

    if (!exportDirectory.filter(path -> !isUnderODKFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_ODK_DEVICE_DIRECTORY);

    if (!exportDirectory.filter(path -> !isUnderBriefcaseFolder(path.toFile())).isPresent())
      errors.add(DIR_INSIDE_BRIEFCASE_STORAGE);

    if (dateRangeStart.isPresent() && !dateRangeEnd.isPresent())
      errors.add("Missing date range end definition");
    if (!dateRangeStart.isPresent() && dateRangeEnd.isPresent())
      errors.add("Missing date range start definition");
    if (!isDateRangeValid())
      errors.add(INVALID_DATE_RANGE_MESSAGE);
    return errors;
  }

  public boolean isValid() {
    return getErrors().isEmpty();
  }

  public <T> Optional<T> mapPemFile(Function<Path, T> mapper) {
    return pemFile.map(mapper);
  }

  public <T> Optional<T> mapExportDir(Function<Path, T> mapper) {
    return exportDirectory.map(mapper);
  }

  public <T> Optional<T> mapDateRangeStart(Function<LocalDate, T> mapper) {
    return dateRangeStart.map(mapper);
  }

  public <T> Optional<T> mapDateRangeEnd(Function<LocalDate, T> mapper) {
    return dateRangeEnd.map(mapper);
  }

  @Override
  public String toString() {
    return "ExportConfiguration{" +
        "exportDirectory=" + exportDirectory +
        ", pemFile=" + pemFile +
        ", dateRangeStart=" + dateRangeStart +
        ", dateRangeEnd=" + dateRangeEnd +
        '}';
  }
}
