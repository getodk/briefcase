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

import static org.opendatakit.briefcase.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.export.ExportOutcome.SOME_SKIPPED;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.bushe.swing.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportProcessTracker {
  private static final Logger log = LoggerFactory.getLogger(ExportProcessTracker.class);

  private final String formId;
  private long start = System.nanoTime();
  long total = 0;
  long exported;
  private int lastReportedPercentage = 0;

  ExportProcessTracker(String formId) {
    this.formId = formId;
  }

  synchronized void incAndReport() {
    exported++;
    int percentage = (int) (exported * 100 / total);
    if (percentage > lastReportedPercentage) {
      EventBus.publish(ExportEvent.progress(percentage / 100D, formId));
      lastReportedPercentage = percentage;
    }
  }

  ExportOutcome computeOutcome() {
    return exported == total
        ? ALL_EXPORTED
        : exported < total
        ? SOME_SKIPPED
        : ALL_SKIPPED;
  }

  public void start() {
    exported = 0;
    start = System.nanoTime();
    EventBus.publish(ExportEvent.start(formId));
  }

  public void end() {
    long end = System.nanoTime();
    LocalTime duration = LocalTime.ofNanoOfDay(end - start);
    log.info("Exported in {}", duration.format(DateTimeFormatter.ISO_TIME));
    EventBus.publish(ExportEvent.end(exported, formId));
  }

  void trackTotal(int total) {
    this.total = total;
  }
}
