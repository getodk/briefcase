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

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.reused.TriStateBoolean;

public class ExportConfigurationBuilder {
  private String exportFilename;
  private Path exportDir;
  private Path pemFile;
  private LocalDate startDate;
  private LocalDate endDate;
  private OverridableBoolean pullBefore = OverridableBoolean.empty();
  private OverridableBoolean overwriteFiles = OverridableBoolean.empty();
  private OverridableBoolean exportMedia = OverridableBoolean.empty();
  private OverridableBoolean splitSelectMultiples = OverridableBoolean.empty();

  public static ExportConfigurationBuilder empty() {
    return new ExportConfigurationBuilder();
  }

  public ExportConfiguration build() {
    return new ExportConfiguration(
        Optional.ofNullable(exportFilename),
        Optional.ofNullable(exportDir),
        Optional.ofNullable(pemFile),
        DateRange.from(startDate, endDate),
        pullBefore,
        overwriteFiles,
        exportMedia,
        splitSelectMultiples
    );
  }

  public ExportConfigurationBuilder setExportFilename(String fileName) {
    exportFilename = fileName;
    return this;
  }

  public ExportConfigurationBuilder setExportDir(Path path) {
    exportDir = path;
    return this;
  }

  public ExportConfigurationBuilder setPemFile(Path path) {
    pemFile = path;
    return this;
  }

  public ExportConfigurationBuilder setStartDate(LocalDate date) {
    startDate = date;
    return this;
  }

  public ExportConfigurationBuilder setEndDate(LocalDate date) {
    endDate = date;
    return this;
  }

  public ExportConfigurationBuilder setPullBefore(boolean value) {
    pullBefore = pullBefore.set(value);
    return this;
  }

  public ExportConfigurationBuilder setOverwriteFiles(boolean value) {
    overwriteFiles = overwriteFiles.set(value);
    return this;
  }

  public ExportConfigurationBuilder setExportMedia(boolean value) {
    exportMedia = exportMedia.set(value);
    return this;
  }

  public ExportConfigurationBuilder setSplitSelectMultiples(boolean value) {
    splitSelectMultiples = splitSelectMultiples.set(value);
    return this;
  }

  public ExportConfigurationBuilder overridePullBefore(TriStateBoolean overrideValue) {
    pullBefore = pullBefore.overrideWith(overrideValue);
    return this;
  }

  public ExportConfigurationBuilder overrideOverwriteFiles(TriStateBoolean overrideValue) {
    overwriteFiles = overwriteFiles.overrideWith(overrideValue);
    return this;
  }

  public ExportConfigurationBuilder overrideExportMedia(TriStateBoolean overrideValue) {
    exportMedia = exportMedia.overrideWith(overrideValue);
    return this;
  }

  public ExportConfigurationBuilder overrideSplitSelectMultiples(TriStateBoolean overrideValue) {
    splitSelectMultiples = splitSelectMultiples.overrideWith(overrideValue);
    return this;
  }
}
