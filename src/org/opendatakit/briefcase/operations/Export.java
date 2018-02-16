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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.bouncycastle.openssl.PEMReader;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.util.ExportToCsv;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Export {
  private static final Logger log = LoggerFactory.getLogger(Export.class);
  private static final Param<Void> EXPORT = Param.flag("e", "export", "Export a form");
  private static final Param<Path> EXPORT_DIR = Param.arg("ed", "export_directory", "Export directory", Paths::get);
  private static final Param<String> FILE = Param.arg("f", "export_filename", "Filename for export operation");
  private static final Param<LocalDate> START = Param.arg("start", "export_start_date", "Export start date", LocalDate::parse);
  private static final Param<LocalDate> END = Param.arg("end", "export_end_date", "Export end date", LocalDate::parse);
  private static final Param<Void> EXCLUDE_MEDIA = Param.flag("em", "exclude_media_export", "Exclude media in export");
  private static final Param<Void> OVERWRITE = Param.flag("oc", "overwrite_csv_export", "Overwrite files during export");
  private static final Param<Path> PEM_FILE = Param.arg("pf", "pem_file", "PEM file for form decryption", Paths::get);

  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(EXPORT_DIR),
          args.get(FILE),
          !args.has(EXCLUDE_MEDIA),
          args.has(OVERWRITE),
          args.getOptional(START),
          args.getOptional(END),
          args.getOptional(PEM_FILE)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END)
  );

  public static void export(String storageDir, String formid, Path exportPath, String baseFilename, boolean includeMediaFiles, boolean overwriteFiles, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Path> maybePemFile) {
    CliEventsCompanion.attach(log);
    bootCache(storageDir);
    Optional<BriefcaseFormDefinition> maybeFormDefinition = FileSystemUtils.getBriefcaseFormList().stream()
        .filter(form -> form.getFormId().equals(formid))
        .findFirst();

    BriefcaseFormDefinition formDefinition = maybeFormDefinition.orElseThrow(() -> new FormNotFoundException(formid));

    if (formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) {
      Path pemFile = maybePemFile
          .filter(Files::exists)
          .orElseThrow(() -> new BriefcaseException("Missing pem file configuration"));

      try (PEMReader rdr = new PEMReader(new BufferedReader(new InputStreamReader(Files.newInputStream(pemFile), "UTF-8")))) {
        Object o = Optional.ofNullable(rdr.readObject()).orElseThrow(() -> new BriefcaseException("Can't parse Pem file"));

        Optional<PrivateKey> privKey;
        if (o instanceof KeyPair)
          privKey = Optional.of(((KeyPair) o).getPrivate());
        else if (o instanceof PrivateKey)
          privKey = Optional.of((PrivateKey) o);
        else
          privKey = Optional.empty();

        formDefinition.setPrivateKey(privKey.orElseThrow(() -> new BriefcaseException("No private key found on Pem file")));
        EventBus.publish(new ExportProgressEvent("Successfully parsed Pem file", formDefinition));
      } catch (IOException e) {
        throw new BriefcaseException("Can't parse Pem file");
      }
    }

    System.out.println("Exporting form " + formDefinition.getFormName() + " (" + formDefinition.getFormId() + ") to: " + exportPath);
    ExportToCsv.export(exportPath, formDefinition, baseFilename, includeMediaFiles, overwriteFiles, startDate, endDate);

    BriefcasePreferences.forClass(ExportPanel.class).put(buildExportDateTimePrefix(formDefinition.getFormId()), LocalDateTime.now().format(ISO_DATE_TIME));
  }
}
