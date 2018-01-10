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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class ExportConfiguration {
  private Optional<Path> exportDirectory;
  private Optional<Path> pemFile;
  private Optional<LocalDate> dateRangeStart;
  private Optional<LocalDate> dateRangeEnd;

  public ExportConfiguration(Optional<Path> exportDirectory, Optional<Path> pemFile, Optional<LocalDate> dateRangeStart, Optional<LocalDate> dateRangeEnd) {
    this.exportDirectory = exportDirectory;
    this.pemFile = pemFile;
    this.dateRangeStart = dateRangeStart;
    this.dateRangeEnd = dateRangeEnd;
  }

  static ExportConfiguration empty() {
    return new ExportConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public boolean isEmpty() {
    return !exportDirectory.isPresent();
  }

  public void setDateRangeStart(Date date) {
    this.dateRangeStart = Optional.ofNullable(date).map(input -> input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
  }

  public void setDateRangeEnd(Date date) {
    this.dateRangeEnd = Optional.ofNullable(date).map(input -> input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
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

  @Override
  public String toString() {
    return "ExportConfiguration{" +
        "exportDirectory=" + exportDirectory +
        ", pemFile=" + pemFile +
        ", dateRangeStart=" + dateRangeStart +
        ", dateRangeEnd=" + dateRangeEnd +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ExportConfiguration that = (ExportConfiguration) o;
    return Objects.equals(exportDirectory, that.exportDirectory) &&
        Objects.equals(pemFile, that.pemFile) &&
        Objects.equals(dateRangeStart, that.dateRangeStart) &&
        Objects.equals(dateRangeEnd, that.dateRangeEnd);
  }

  @Override
  public int hashCode() {

    return Objects.hash(exportDirectory, pemFile, dateRangeStart, dateRangeEnd);
  }

  public boolean isValid() {
    return getErrors().isEmpty();
  }

  public List<String> getErrors() {
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

    if (!dateRangeEnd.isPresent())
      errors.add("Missing date range end definition");
    if (!dateRangeStart.isPresent())
      errors.add("Missing date range start definition");
    if (!isDateRangeValid())
      errors.add(INVALID_DATE_RANGE_MESSAGE);
    return errors;
  }
}
