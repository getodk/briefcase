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

  PullFromAggregateTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent() {
    onEventCallback.accept(new FormStatusEvent(form));
  }

  void trackStart() {
    String message = "Start pulling form and submissions";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEnd() {
    String message = "Completed pulling form and submissions";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    form.setStatusString(message);
    log.warn("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartDownloadingForm() {
    String message = "Start downloading form";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorDownloadingForm(Response response) {
    String message = "Error downloading form";
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Pull {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackEndDownloadingForm() {
    String message = "Form downloaded";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartGettingSubmissionIds() {
    String message = "Start getting submission IDs";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndGettingSubmissionIds() {
    String message = "Got all the submission IDs";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartGettingFormManifest() {
    String message = "Start getting form manifest";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorGettingFormManifest(Response response) {
    String message = "Error getting form manifest";
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Pull {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackIgnoredFormAttachments(int attachmentsToDownload, int totalAttachments) {
    if (attachmentsToDownload < totalAttachments) {
      String message = "Skipping " + (totalAttachments - attachmentsToDownload) + " form attachments that have been already downloaded";
      form.setStatusString(message);
      log.info("Pull {} - {}", form.getFormName(), message);
      notifyTrackingEvent();
    }
  }

  void trackSubmissionAlreadyDownloaded(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": already downloaded";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Start downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndDownloadingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " downloaded";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorDownloadingFormAttachment(int attachmentNumber, int totalAttachments, Response response) {
    String message = "Error downloading form attachment " + attachmentNumber + " of " + totalAttachments;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Pull {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackStartDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Start downloading submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorDownloadingSubmission(int submissionNumber, int totalSubmissions, Response response) {
    String message = "Error downloading submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Pull {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackEndDownloadingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Start downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " downloaded";
    form.setStatusString(message);
    log.info("Pull {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorDownloadingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments, Response response) {
    String message = "Error downloading attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Pull {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to download";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }
}
