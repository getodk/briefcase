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

package org.opendatakit.briefcase.operations.transfer.push.central;

import static org.opendatakit.briefcase.reused.model.Operation.PUSH;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushToCentralTracker {
  private static final Logger log = LoggerFactory.getLogger(PushToCentralTracker.class);
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadata formMetadata;
  private boolean errored = false;

  PushToCentralTracker(Consumer<FormStatusEvent> onEventCallback, FormMetadata formMetadata) {
    this.formMetadata = formMetadata;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String message) {
    onEventCallback.accept(new FormStatusEvent(PUSH, formMetadata.getKey(), message));
  }

  // TODO v2.0 Move this factory to the CentralErrorMessage class
  private static CentralErrorMessage parseErrorResponse(String errorResponse) {
    if (errorResponse.isEmpty())
      return CentralErrorMessage.empty();
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(errorResponse);
      String message = jsonNode.get("message").asText();

      return new CentralErrorMessage(message);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void trackErrorCheckingForm(Response response) {
    errored = true;
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error checking if form exists in Aggregate";
    log.error("Push {} - {}: HTTP {} {} {}", formMetadata.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage.message);
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackCancellation(String job) {
    String message = "Operation cancelled - " + job;
    log.warn("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEncryptedForm() {
    String message = "Encrypted form - Can't push to Central";
    log.warn(message);
    notifyTrackingEvent(message);
  }

  void trackStart() {
    String message = "Start pushing form and submissions";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackNoSubmissions() {
    String message = "There are no submissions to send";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartSendingForm() {
    String message = "Sending form";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingForm() {
    String message = "Form sent";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackFormAlreadyExists() {
    String message = "Skipping form: already exists";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingForm(Response response) {
    errored = true;
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending form";
    log.error("Push {} - {} HTTP {} {} {}", formMetadata.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage.message);
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackStartSendingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Sending form attachment " + attachmentNumber + " of " + totalAttachments;
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingFormAttachment(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " sent";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackFormAttachmentAlreadyExists(int attachmentNumber, int totalAttachments) {
    String message = "Skipping form attachment " + attachmentNumber + " of " + totalAttachments + ": already exists";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingFormAttachment(int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending form attachment " + attachmentNumber + " of " + totalAttachments;
    log.error("Push {} - {} HTTP {} {} {}", formMetadata.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage.message);
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackStartSendingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Sending submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingSubmission(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " sent";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingSubmission(int submissionNumber, int totalSubmissions, Response response) {
    errored = true;
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Push {} - {} HTTP {} {} {}", formMetadata.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage);
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }

  void trackSubmissionAlreadyExists(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": already exists";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackNoInstanceId(int submissionNumber, int totalSubmissions) {
    String message = "Skipping submission " + submissionNumber + " of " + totalSubmissions + ": missing instance ID";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackStartSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Sending attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackEndSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " sent";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackSubmissionAttachmentAlreadyExists(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Skipping attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + ": already exists";
    log.info("Push {} - {}", formMetadata.getFormName(), message);
    notifyTrackingEvent(message);
  }

  void trackErrorSendingSubmissionAttachment(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments, Response response) {
    errored = true;
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    String message = "Error sending attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions;
    log.error("Push {} - {} HTTP {} {} {}", formMetadata.getFormName(), message, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage.message);
    notifyTrackingEvent(message + ": " + response.getStatusPhrase());
  }
}
