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

  PushToAggregateTracker(FormStatus form, Consumer<FormStatusEvent> onEventCallback) {
    this.form = form;
    this.onEventCallback = onEventCallback;
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

  void trackStartPushing() {
    form.setStatusString("Starting to push form");
    log.info("Starting to push form");
    notifyTrackingEvent();
  }

  void trackPushForm(int attachments) {
    form.setStatusString("Pushed form with " + attachments + " attachments");
    log.info("Pushed form with {} attachments", attachments);
    notifyTrackingEvent();
  }

  void trackPushSubmission(int attachments, String instanceId) {
    form.setStatusString("Pushed submission " + instanceId + " with " + attachments + " attachments");
    log.info("Pushed submission {} with {} attachments", instanceId, attachments);
    notifyTrackingEvent();
  }

  void trackFormAlreadyExists(boolean exists) {
    String message = exists ? "Form already exists in Aggregate" : "Form doesn't exist in Aggregate";
    form.setStatusString(message);
    log.info(message);
    notifyTrackingEvent();
  }

  void trackForceSendForm(boolean forceSendForm) {
    if (forceSendForm) {
      form.setStatusString("Forcing push of form and attachments");
      log.info("Forcing push of form and attachments");
      notifyTrackingEvent();
    }
  }
}
