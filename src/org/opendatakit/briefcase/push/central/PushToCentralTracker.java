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

package org.opendatakit.briefcase.push.central;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushToCentralTracker {
  private static final Logger log = LoggerFactory.getLogger(PushToCentralTracker.class);
  private final FormStatus form;
  private final Consumer<FormStatusEvent> onEventCallback;
  private boolean errored = false;

  PushToCentralTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent() {
    onEventCallback.accept(new FormStatusEvent(form));
  }

  // TODO v2.0 Move this factory to the CentralErrorMessage class
  private static String parseErrorResponse(String errorResponse) {
    if (errorResponse.isEmpty())
      return "";
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(errorResponse);
      String message = jsonNode.get("message").asText();

      return message;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void trackErrorCheckingForm(Response response) {
    errored = true;
    String centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error checking if form exists in Aggregate";
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {}: HTTP {} {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent();
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    form.setStatusString(message);
    log.warn("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEncryptedForm() {
    String message = "Encrypted form - Can't push to Central";
    form.setStatusString(message);
    log.warn(message);
    notifyTrackingEvent();
  }

  void trackCannotDetermineEncryption(Throwable t) {
    String message = "Can't determine form encryption";
    form.setStatusString(message + ":" + t.getMessage());
    log.error(message, t);
    notifyTrackingEvent();
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

  void trackNoSubmissions() {
    String message = "There are no submissions to send";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartSendingForm() {
    String message = "Sending form";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingForm() {
    String message = "Form sent";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackFormAlreadyExists() {
    String message = "Skipping form: already exists";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingForm(Response response) {
    errored = true;
    String centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending form";
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent();
  }

  void trackStartSendingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Sending form attachment " + attachmentNumber + " of " + totalAttachments;
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " sent";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackFormAttachmentAlreadyExists(int attachmentNumber, int totalAttachments) {
    String message = "Skipping form attachment " + attachmentNumber + " of " + totalAttachments + ": already exists";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingFormAttachment(int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending form attachment " + attachmentNumber + " of " + totalAttachments;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent();
  }

  void trackStartSendingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Sending submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " sent";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingSubmission(int submissionNumber, int totalSubmissions, Response response) {
    errored = true;
    String centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent();
  }

  void trackSubmissionAlreadyExists(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": already exists";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackNoInstanceId(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": missing instance ID";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackStartSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Sending attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackEndSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " sent";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackSubmissionAttachmentAlreadyExists(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Skipping attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + ": already exists";
    form.setStatusString(message);
    log.info("Push {} - {}", form.getFormName(), message);
    notifyTrackingEvent();
  }

  void trackErrorSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    String centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    form.setStatusString(message + ": " + response.getStatusPhrase());
    log.error("Push {} - {} HTTP {} {} {}", form.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent();
  }
}
