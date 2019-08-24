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

import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushToAggregateTracker {
  private static final Logger log = LoggerFactory.getLogger(PushToAggregateTracker.class);
  private final FormStatus form;
  private final Consumer<FormStatusEvent> onEventCallback;
  private boolean errored = false;

  PushToAggregateTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent() {
    onEventCallback.accept(new FormStatusEvent(form.getFormMetadata().getKey(), form.getStatusString()));
  }

  void trackStart() {
    String message = "Start pushing form and submissions";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorCheckingForm(Response response) {
    errored = true;
    String message = "Error checking if form exists in Aggregate";
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {}: HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    form.setStatusString(message);
    log.warn("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackFormAlreadyExists(boolean exists) {
    String message = exists ? "Form already exists in Aggregate" : "Form doesn't exist in Aggregate";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackForceSendForm(boolean forceSendForm) {
    if (forceSendForm) {
      String message = "Forcing push of form";
      form.setStatusString(message);
      log.info("Push {} - {}", form.getFormName(), message);
      notifyTrackingEvent();
    }
  }

  void trackStartSendingFormAndAttachments(int part, int parts) {
    String message = "Sending form" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingFormAndAttachments(int part, int parts) {
    String message = "Form sent" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingFormAndAttachments(int part, int parts, Response response) {
    errored = true;
    String message = "Error sending form" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackStartSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts) {
    String message = "Sending submission " + submissionNumber + " of " + totalSubmissions + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " sent" + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingSubmissionAndAttachments(int submissionNumber, int totalSubmissions, int part, int parts, Response response) {
    errored = true;
    String message = "Error sending submission " + submissionNumber + " of " + totalSubmissions + (parts == 1 ? "" : " (" + part + "/" + parts + ")");
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase());
    notifyTrackingEvent();
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to send";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }
}
