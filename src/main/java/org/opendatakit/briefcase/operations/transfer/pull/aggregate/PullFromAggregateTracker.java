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

package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static org.opendatakit.briefcase.reused.model.Operation.PULL;

import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PullFromAggregateTracker {
  private static final Logger log = LoggerFactory.getLogger(PullFromAggregateTracker.class);
  private final FormKey key;
  private final Consumer<FormStatusEvent> onEventCallback;
  private boolean errored = false;

  PullFromAggregateTracker(FormKey key, Consumer<FormStatusEvent> onEventCallback) {
    this.key = key;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String statusString) {
    onEventCallback.accept(new FormStatusEvent(PULL, key, statusString));
  }

  void trackStart() {
    String message = "Start pulling form and submissions";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    log.warn("Push {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingForm() {
    String message = "Start downloading form";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingForm(Response response) {
    errored = true;
    String message = "Error downloading form";
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackEndDownloadingForm() {
    String message = "Form downloaded";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartGettingSubmissionIds() {
    String message = "Start getting submission IDs";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndGettingSubmissionIds() {
    String message = "Got all the submission IDs";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartGettingFormManifest() {
    String message = "Start getting form manifest";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndGettingFormManifest() {
    String message = "Got the form manifest";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorGettingFormManifest(Response response) {
    errored = true;
    String message = "Error getting form manifest";
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackIgnoredFormAttachments(int attachmentsToDownload, int totalAttachments) {
    if (attachmentsToDownload < totalAttachments) {
      String message = "Skipping " + (totalAttachments - attachmentsToDownload) + " form attachments that have been already downloaded";
      log.info("Pull {} - {}", key.getName(), message);
      notifyTrackingEvent(message);
    }
  }

  void trackSubmissionAlreadyDownloaded(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": already downloaded";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Start downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " downloaded";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingFormAttachment(int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String message = "Error downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackStartDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Start downloading submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingSubmission(int submissionNumber, int totalSubmissions, Response response) {
    errored = true;
    String message = "Error downloading submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackEndDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Start downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    log.info("Pull {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String message = "Error downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to download";
    log.info("Push {} - {}", key.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorGettingInstanceIdBatches(Response response) {
    errored = true;
    String message = "Error getting batches of instance IDs";
    log.error("Pull {} - {}: HTTP {} {}", key.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }
}
