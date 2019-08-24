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

package org.opendatakit.briefcase.pull.central;

import static org.opendatakit.briefcase.reused.Operation.PULL;

import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PullFromCentralTracker {
  private static final Logger log = LoggerFactory.getLogger(PullFromCentralTracker.class);
  private final FormKey formKey;
  private final Consumer<FormStatusEvent> onEventCallback;
  private boolean errored = false;

  PullFromCentralTracker(FormKey key, Consumer<FormStatusEvent> onEventCallback) {
    this.formKey = key;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String message) {
    onEventCallback.accept(new FormStatusEvent(PULL, formKey, message));
  }

  void trackStart() {
    String message = "Start pulling form and submissions";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    log.warn("Push {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingForm() {
    String message = "Start downloading form";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingForm(Response response) {
    errored = true;
    String message = "Error downloading form";
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackEndDownloadingForm() {
    String message = "Form downloaded";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartGettingSubmissionIds() {
    String message = "Start getting submission IDs";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorGettingSubmissionIds(Response response) {
    errored = true;
    String message = "Error getting submission IDs";
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }


  void trackEndGettingSubmissionIds() {
    String message = "Got all the submission IDs";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartGettingFormAttachments() {
    String message = "Start getting form attachments";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndGettingFormAttachments() {
    String message = "Got all form attachments";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorGettingFormAttachments(Response response) {
    errored = true;
    String message = "Error getting form attachments";
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackNonExistingFormAttachments(int existingAttachments, int totalAttachments) {
    if (existingAttachments < totalAttachments) {
      String message = "Server is missing " + (totalAttachments - existingAttachments) + " form attachments";
      log.info("Pull {} - {}", formKey.getName(), message);
      notifyTrackingEvent(message);
    }
  }

  void trackSubmissionAlreadyDownloaded(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": already downloaded";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Start downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " downloaded";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingFormAttachment(int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String message = "Error downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackStartDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Start downloading submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingSubmission(int submissionNumber, int totalSubmissions, Response response) {
    errored = true;
    String message = "Error downloading submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackEndDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartGettingSubmissionAttachments(int submissionNumber, int totalSubmissions) {
    String message = "Start getting attachments of submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorGettingSubmissionAttachments(int submissionNumber, int totalSubmissions, Response response) {
    errored = true;
    String message = "Error getting attachments of submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }


  void trackEndGettingSubmissionAttachments(int submissionNumber, int totalSubmissions) {
    String message = "Got all the attachments of submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Start downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    log.info("Pull {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String message = "Error downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Pull {} - {}: HTTP {} {}", formKey.getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to download";
    log.info("Push {} - {}", formKey.getName(), message);
    notifyTrackingEvent(message);
  }
}
