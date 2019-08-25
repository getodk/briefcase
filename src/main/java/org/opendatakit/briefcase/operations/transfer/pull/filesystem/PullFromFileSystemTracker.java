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

package org.opendatakit.briefcase.operations.transfer.pull.filesystem;

import static org.opendatakit.briefcase.reused.model.Operation.PULL;

import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PullFromFileSystemTracker {
  private static final Logger log = LoggerFactory.getLogger(PullFromFileSystemTracker.class);
  private final FormKey formKey;
  private final Consumer<FormStatusEvent> onEventCallback;

  PullFromFileSystemTracker(FormKey formKey, Consumer<FormStatusEvent> onEventCallback) {
    this.formKey = formKey;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String message) {
    onEventCallback.accept(new FormStatusEvent(PULL, formKey, message));
  }

  void trackStart() {
    String message = "Start pulling form and submissions";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = "Success";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }

  void trackFormInstalled() {
    String message = "Form installer";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }

  void trackFormAttachmentInstaller(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " installed";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }

  void trackSubmissionInstalled(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " installed";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }

  void trackSubmissionAttachmentInstalled(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " installed";
    log.info("Pull {} - {}", formKey.getId(), message);
    notifyTrackingEvent(message);
  }
}
