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

import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;

/**
 * This class will handle Events while we change from an annotation based,
 * static API Event bus to something more flexible.
 */
class CliEventsCompanion {
  static void attach(Log log) {
    on(ExportProgressEvent.class, (ExportProgressEvent event) -> {
      if (event.getText().contains("Cause:")) {
        log.warn(event.getText());
        System.err.println(event.getText());
      } else {
        log.info(event.getText());
        System.out.println(event.getText());
      }
    });

    on(ExportSucceededEvent.class, (ExportSucceededEvent event) -> {
      log.info("Succeeded.");
      System.out.println("Succeeded.");
    });

    on(TransferSucceededEvent.class, (TransferSucceededEvent event) -> {
      log.info("Transfer Succeeded");
      System.out.println("Transfer Succeeded");
    });

    on(FormStatusEvent.class, (FormStatusEvent fse) -> {
      log.info(fse.getStatusString());
      System.out.println(fse.getStatusString());
    });

    on(StartPullEvent.class, (StartPullEvent event) -> {
      log.info("Start pull form " + event.form.getFormName());
      System.out.println("Start pull form " + event.form.getFormName());
    });

    on(ExportFailedEvent.class, (ExportFailedEvent event) -> {
      log.error("Failed.");
      System.err.println("Failed.");
      System.exit(1);
    });

    on(WrongExportConfigurationEvent.class, (WrongExportConfigurationEvent event) -> {
      log.error("Wrong export configuration: " + event.cause);
      System.err.println("Wrong export configuration: " + event.cause);
      System.exit(1);
    });

    on(TransferFailedEvent.class, (TransferFailedEvent event) -> {
      log.error("Transfer Failed");
      System.err.println("Transfer Failed.");
      System.exit(1);
    });

    on(RetrieveAvailableFormsFailedEvent.class, (RetrieveAvailableFormsFailedEvent event) -> {
      log.error("Accessing the server failed with error: " + event.getReason());
      System.err.println("Accessing the server failed with error: " + event.getReason());
      System.exit(1);
    });

    on(ServerConnectionTestFailedEvent.class, (ServerConnectionTestFailedEvent event) -> {
      log.error("Server connection test failed");
      System.err.println("Server connection test failed");
      System.exit(1);
    });

    on(FormNotFoundEvent.class, (FormNotFoundEvent event) -> {
      log.error("Form not found");
      System.err.println("Form not found");
      System.exit(1);
    });

  }

  @SuppressWarnings("unchecked")
  private static <T> void on(Class<T> eventClass, Consumer<T> callback) {
    EventBus.subscribe(eventClass, o -> callback.accept((T) o));
  }
}
