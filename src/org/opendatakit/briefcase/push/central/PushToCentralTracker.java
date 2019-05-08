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

import static java.util.Collections.emptyMap;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
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

  PushToCentralTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent() {
    onEventCallback.accept(new FormStatusEvent(form));
  }

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

  void trackError(String error, Response response) {
    CentralErrorMessage centralErrorMessage = parseErrorResponse(response.getServerErrorResponse());
    form.setStatusString(error + ": " + response.getStatusPhrase());
    log.error("{}: HTTP {} {} {}", error, response.getStatusCode(), response.getStatusPhrase(), centralErrorMessage.message);
    notifyTrackingEvent();
  }

  void trackCancellation(String job) {
    form.setStatusString("Operation cancelled - " + job);
    log.warn("Operation cancelled - {}", job);
    notifyTrackingEvent();
  }

  void trackPushForm() {
    form.setStatusString("Pushed form");
    log.info("Pushed form");
    notifyTrackingEvent();
  }

  void trackPushFormAttachment(Path attachment) {
    form.setStatusString("Pushed form attachment " + attachment.getFileName());
    log.info("Pushed form attachment {}", attachment.getFileName());
    notifyTrackingEvent();
  }

  void pushSubmission(String instanceId) {
    form.setStatusString("Pushed submission " + instanceId);
    log.info("Pushed submission {}", instanceId);
    notifyTrackingEvent();
  }

  void trackPushSubmissionAttachment(Path attachment, String instanceId) {
    form.setStatusString("Pushed attachment " + attachment.getFileName() + " of submission " + instanceId);
    log.info("Pushed attachment {} of submission {}", attachment.getFileName(), instanceId);
    notifyTrackingEvent();
  }

  void trackFormAlreadyExists(boolean exists) {
    String message = exists ? "Form already exists in Central" : "Form doesn't exist in Central";
    form.setStatusString(message);
    log.info(message);
    notifyTrackingEvent();
  }

  void trackFormAlreadyExists() {
    form.setStatusString("Skipping: form already exists in Central");
    log.info("Skipping: form already exists in Central");
    notifyTrackingEvent();
  }

  void trackFormAttachmentAlreadyExists(Path attachment) {
    form.setStatusString("Skipping: form attachment " + attachment.getFileName() + " already exists in Central");
    log.info("Skipping: form attachment {} already exists in Central", attachment.getFileName());
    notifyTrackingEvent();
  }

  void trackSubmissionAlreadyExists(String instanceId) {
    form.setStatusString("Skipping: submission " + instanceId + " already exists in Central");
    log.info("Skipping: submission {} already exists in Central", instanceId);
    notifyTrackingEvent();
  }

  void trackSubmissionAttachmentAlreadyExists(Path attachment, String instanceId) {
    form.setStatusString("Skipping: attachment " + attachment.getFileName() + " of submission " + instanceId + " already exists in Central");
    log.info("Skipping: attachment {} of submission {} already exists in Central", attachment.getFileName(), instanceId);
    notifyTrackingEvent();
  }
}
