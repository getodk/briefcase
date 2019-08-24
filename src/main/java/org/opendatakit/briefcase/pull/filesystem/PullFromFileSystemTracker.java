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

package org.opendatakit.briefcase.pull.filesystem;

import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PullFromFileSystemTracker {
  private static final Logger log = LoggerFactory.getLogger(PullFromFileSystemTracker.class);
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadata formMetadata;
  private boolean errored = false;

  PullFromFileSystemTracker(FormMetadata formMetadata, Consumer<FormStatusEvent> onEventCallback) {
    this.formMetadata = formMetadata;
    this.onEventCallback = onEventCallback;
  }

  private void notifyTrackingEvent(String message) {
    onEventCallback.accept(new FormStatusEvent(formMetadata.getKey(), message));
  }

  void trackStart() {
    String message = "Start pulling form and submissions";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackEnd() {
    String message = errored ? "Success with errors" : "Success";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackFormInstalled() {
    String message = "Form installer";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackFormAttachmentInstaller(int attachmentNumber, int totalAttachments) {
    String message = "Form attachment " + attachmentNumber + " of " + totalAttachments + " installed";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  void trackSubmissionInstalled(int submissionNumber, int totalSubmissions) {
    String message = "Submission " + submissionNumber + " of " + totalSubmissions + " installed";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }

  public void trackSubmissionAttachmentInstalled(int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    String message = "Attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions + " installed";
    log.info("Pull {} - {}", formMetadata.getKey().getName(), message);
    notifyTrackingEvent(message);
  }
}
