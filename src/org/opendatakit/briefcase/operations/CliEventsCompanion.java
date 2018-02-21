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
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.slf4j.Logger;

/**
 * This class will handle Events while we change from an annotation based,
 * static API Event bus to something more flexible.
 */
class CliEventsCompanion {
  static void attach(Logger log) {
    on(ExportProgressEvent.class, event -> {
      if (event.getText().contains("Cause:")) {
        log.warn(event.getText());
        System.err.println(event.getText());
      } else {
        log.info(event.getText());
        System.out.println(event.getText());
      }
    });

    on(ExportSucceededEvent.class, event -> {
      log.info("Succeeded.");
      System.out.println("Succeeded.");
    });

    on(TransferSucceededEvent.class, event -> {
      log.info("Transfer Succeeded");
      System.out.println("Transfer Succeeded");
    });

    on(FormStatusEvent.class, fse -> {
      log.info(fse.getStatusString());
      System.out.println(fse.getStatusString());
    });

    on(StartPullEvent.class, event -> {
      log.info("Start pull form " + event.form.getFormName());
      System.out.println("Start pull form " + event.form.getFormName());
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> void on(Class<T> eventClass, Consumer<T> callback) {
    EventBus.subscribe(eventClass, o -> callback.accept((T) o));
  }
}
