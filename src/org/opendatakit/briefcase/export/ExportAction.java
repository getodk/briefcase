/*
 * Copyright (C) 2011 University of Washington.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.ErrorsOr;
import org.opendatakit.briefcase.util.ExportToCsv;

public class ExportAction {
  private static final Log log = LogFactory.getLog(ExportAction.class);

  private static Optional<PrivateKey> extractPrivateKey(Object o) {
    if (o instanceof KeyPair)
      return Optional.of(((KeyPair) o).getPrivate());
    if (o instanceof PrivateKey)
      return Optional.of((PrivateKey) o);
    return Optional.empty();
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
      log.error(e);
      return ErrorsOr.errors("Briefcase can't read the provided file: " + e.getMessage());
    }
  }

  public static void export(BriefcaseFormDefinition formDefinition, ExportConfiguration configuration, TerminationFuture terminationFuture) {
    if (formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) {
      formDefinition.setPrivateKey(readPemFile(configuration.getPemFile()
          .orElseThrow(() -> new RuntimeException("PEM file not present"))
      ).get());
    }
    ExportToCsv action = new ExportToCsv(
        configuration.getExportDir().orElseThrow(() -> new RuntimeException("Export dir not present")).toFile(),
        formDefinition,
        terminationFuture,
        configuration.mapStartDate((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null),
        configuration.mapEndDate((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null)
    );
    try {
      boolean allSuccessful = action.doAction();

      if (!allSuccessful)
        EventBus.publish(new ExportFailedEvent(action.getFormDefinition()));

      if (allSuccessful && action.noneSkipped())
        EventBus.publish(new ExportSucceededEvent(action.getFormDefinition()));

      if (allSuccessful && action.someSkipped())
        EventBus.publish(new ExportSucceededWithErrorsEvent(action.getFormDefinition()));

      if (allSuccessful && action.allSkipped())
        EventBus.publish(new ExportFailedEvent(action.getFormDefinition()));
    } catch (Exception e) {
      log.error("export action failed", e);
      EventBus.publish(new ExportFailedEvent(action.getFormDefinition()));
    }
  }

}
