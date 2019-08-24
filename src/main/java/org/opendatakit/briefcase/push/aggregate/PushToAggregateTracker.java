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

package org.opendatakit.briefcase.push.aggregate;

import static org.opendatakit.briefcase.reused.Operation.PUSH;

import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushToAggregateTracker {
  private static final Logger log = LoggerFactory.getLogger(PushToAggregateTracker.class);
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadata formMetadata;
  private boolean errored = false;

  PushToAggregateTracker(Consumer<FormStatusEvent> onEventCallback, FormMetadata formMetadata) {
    this.formMetadata = formMetadata;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String message) {
    onEventCallback.accept(new FormStatusEvent(PUSH, formMetadata.getKey(), message));
  }

  void trackStart() {
    String message = "Start pushing form and submissions";
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorCheckingForm(Response response) {
    errored = true;
    String message = "Error checking if form exists in Aggregate";
    log.error("Push {} - {}: HTTP {} {}", formMetadata.getKey().getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    log.warn("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackFormAlreadyExists(boolean exists) {
    String message = exists ? "Form already exists in Aggregate" : "Form doesn't exist in Aggregate";
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackForceSendForm(boolean forceSendForm) {
    if (forceSendForm) {
      String message = "Forcing push of form";
      log.info("Push {} - {}", formMetadata.getKey().getName(), message);
      notifyTrackingEvent(message);
    }
  }

  void trackStartSendingFormAndAttachments(int part, int parts) {
    String message = "Sending form" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingFormAndAttachments(int part, int parts) {
    String message = "Form sent" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingFormAndAttachments(int part, int parts, Response response) {
    errored = true;
    String message = "Error sending form" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.error("Push {} - {} HTTP {} {}", formMetadata.getKey().getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackStartSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts) {
    String message = "Sending submission " + submissionNumber + " of " + totalSubmissions + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " sent" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts, Response response) {
    errored = true;
    String message = "Error sending submission " + submissionNumber + " of " + totalSubmissions + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    log.error("Push {} - {} HTTP {} {}", formMetadata.getKey().getName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to send";
    log.info("Push {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }
}
