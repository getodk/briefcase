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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.ExportToCsv;
import org.opendatakit.briefcase.util.PrivateKeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportAction {
  private static final Logger log = LoggerFactory.getLogger(ExportAction.class);

  public static void export(BriefcaseFormDefinition formDefinition, ExportConfiguration configuration, TerminationFuture terminationFuture) {
    if (formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) {
      formDefinition.setPrivateKey(PrivateKeyUtils.readPemFile(configuration.getPemFile()
          .orElseThrow(() -> new RuntimeException("PEM file not present"))
      ).get());
    }
    ExportToCsv action = new ExportToCsv(
        terminationFuture,
        configuration.getExportDir().orElseThrow(() -> new RuntimeException("Export dir not present")).toFile(),
        formDefinition,
        formDefinition.getFormName(),
        true,
        false,
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
