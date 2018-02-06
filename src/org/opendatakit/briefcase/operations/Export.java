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
package org.opendatakit.briefcase.operations;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.opendatakit.briefcase.export.ExportForms.buildExportDateTimePrefix;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.export.ExportAction;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class Export {
  private static final Log LOGGER = LogFactory.getLog(Export.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
  private static final Param<Void> EXPORT = Param.flag("e", "export", "Export a form");
  private static final Param<String> EXPORT_DIR = Param.arg("ed", "export_directory", "Export directory");
  private static final Param<String> FILE = Param.arg("f", "export_filename", "Filename for export operation");
  private static final Param<Date> START = Param.arg("start", "export_start_date", "Export start date", Export::toDate);
  private static final Param<Date> END = Param.arg("end", "export_end_date", "Export end date", Export::toDate);
  private static final Param<Void> EXCLUDE_MEDIA = Param.flag("em", "exclude_media_export", "Exclude media in export");
  private static final Param<Void> OVERWRITE = Param.flag("oc", "overwrite_csv_export", "Overwrite files during export");
  private static final Param<String> PEM_FILE = Param.arg("pf", "pem_file", "PEM file for form decryption");

  public static Date toDate(String s) {
    try {
      return DATE_FORMAT.parse(s);
    } catch (ParseException e) {
      LOGGER.error("bad date range", e);
      return null;
    }
  }

  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(EXPORT_DIR),
          args.getOrNull(START),
          args.getOrNull(END),
          args.getOptional(PEM_FILE)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END)
  );

  public static void export(String storageDir, String formId, String exportDir, Date startDateString, Date endDateString, Optional<String> pemFileLocation) {
    bootCache(storageDir);

    Optional<BriefcaseFormDefinition> maybeFormDefinition = FileSystemUtils.formCache.getForm(formId);
    if (!maybeFormDefinition.isPresent()) {
      System.err.println("Form not found");
      System.exit(1);
    }

    BriefcaseFormDefinition formDefinition = maybeFormDefinition.get();

    ExportConfiguration exportConfiguration = new ExportConfiguration(
        Optional.ofNullable(exportDir).map(Paths::get),
        pemFileLocation.map(Paths::get),
        Optional.ofNullable(startDateString).map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
        Optional.ofNullable(endDateString).map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
        Optional.of(false)
    );

    List<String> errors = validateConfiguration(formDefinition, exportConfiguration);
    errors.forEach(System.err::println);
    if (!errors.isEmpty())
      System.exit(1);

    ExportAction.export(formDefinition, exportConfiguration, new TerminationFuture());

    BriefcasePreferences.forClass(ExportPanel.class).put(buildExportDateTimePrefix(formDefinition.getFormId()), LocalDateTime.now().format(ISO_DATE_TIME));
  }

  private static List<String> validateConfiguration(BriefcaseFormDefinition formDefinition, ExportConfiguration exportConfiguration) {
    boolean needsPemFile = formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm();

    if (needsPemFile && !exportConfiguration.isPemFilePresent())
      return Collections.singletonList("The form " + formDefinition.getFormName() + " is encrypted and you haven't set a PEM file");
    if (needsPemFile)
      return ExportAction.readPemFile(exportConfiguration.getPemFile()
          .orElseThrow(() -> new RuntimeException("PEM file not present"))
      ).getErrors();
    return Collections.emptyList();
  }
}
