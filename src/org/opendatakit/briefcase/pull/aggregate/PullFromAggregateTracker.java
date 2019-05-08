/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.pull.aggregate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PullFromAggregateTracker {
  private static final Logger log = LoggerFactory.getLogger(PullFromAggregateTracker.class);
  private final FormStatus form;
  private final Consumer<FormStatusEvent> onEventCallback;
  private int totalSubmissions;
  private AtomicInteger submissionCounter = new AtomicInteger(0);

  PullFromAggregateTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
  }

  void trackFormDownloaded() {
    form.setStatusString("Downloaded form " + form.getFormName());
    log.info("Downloaded form {}", form.getFormName());
    notifyTrackingEvent();
  }

  void trackBatches(List<InstanceIdBatch> batches) {
    totalSubmissions = batches.stream().map(InstanceIdBatch::count).reduce(0, Integer::sum);
    form.setStatusString("Downloading " + totalSubmissions + " submissions");
    log.info("Downloaded {} submissions", totalSubmissions);
    notifyTrackingEvent();
  }

  void trackSubmission() {
    form.setStatusString("Downloaded submission " + submissionCounter.incrementAndGet() + " of " + totalSubmissions);
    log.info("Downloaded submission {} of {}", submissionCounter.get(), totalSubmissions);
    notifyTrackingEvent();
  }

  void trackMediaFiles(int totalAttachments, int attachmentsToDownload) {
    if (totalAttachments > 0) {
      form.setStatusString("Downloading " + attachmentsToDownload + " form attachments");
      log.info("Downloading {} form attachments", attachmentsToDownload);
      if (totalAttachments > attachmentsToDownload) {
        int number = totalAttachments - attachmentsToDownload;
        form.setStatusString("Ignoring " + number + " form attachments (already present)");
        log.info("Ignoring {} form attachments (already present)", number);
      }
      notifyTrackingEvent();
    }
  }

  private void notifyTrackingEvent() {
    onEventCallback.accept(new FormStatusEvent(form));
  }

  void trackError(String error, Response response) {
    form.setStatusString(error + ": " + response.getStatusPhrase());
    log.error("{}: HTTP {} {}", error, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackCancellation(String job) {
    form.setStatusString("Operation cancelled - " + job);
    log.warn("Operation cancelled - {}", job);
    notifyTrackingEvent();
  }

  void formAttachmentDownloaded(AggregateAttachment attachment) {
    form.setStatusString("Downloaded form attachment " + attachment.getFilename());
    log.info("Downloaded form attachment {}", attachment.getFilename());
    notifyTrackingEvent();
  }

  void trackTotalSubmissions(int totalSubmissions) {
    this.totalSubmissions = totalSubmissions;
    form.setStatusString("Downloading " + totalSubmissions + " submissions");
    log.info("Downloading {} submissions", totalSubmissions);
    notifyTrackingEvent();
  }

  void submissionAttachmentDownloaded(String instanceId, AggregateAttachment attachment) {
    form.setStatusString("Downloaded attachment " + attachment.getFilename() + " of submission " + instanceId);
    log.info("Downloaded attachment {} of submission {}", attachment.getFilename(), instanceId);
    notifyTrackingEvent();
  }
}
