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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.list;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionKey;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PushToCentral {

  private final Http http;
  private final SubmissionMetadataPort submissionMetadataPort;
  private final CentralServer server;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PushToCentral(Http http, SubmissionMetadataPort submissionMetadataPort, CentralServer server, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
    this.submissionMetadataPort = submissionMetadataPort;
    this.server = server;
    this.token = token;
    this.onEventCallback = onEventCallback;
  }

  /**
   * Pushes a form completely, sending the form file, form attachments,
   * submission files and their attachments to the Aggregate server.
   * <p>
   * It won't push a form and form attachments if it's already
   * present in the server.
   */
  @SuppressWarnings("checkstyle:Indentation")
  public Job<Void> push(FormMetadata formMetadata) {
    PushToCentralTracker tracker = new PushToCentralTracker(onEventCallback, formMetadata);

    Job<Void> startTrackingJob = Job.run(runnerStatus -> tracker.trackStart());

    if (formMetadata.isEncrypted())
      return startTrackingJob.thenRun(rs -> {
        tracker.trackEncryptedForm();
        tracker.trackEnd();
      });

    return startTrackingJob
        .thenRun(rs -> {
          if (checkFormExists(formMetadata, rs, tracker) && pushForm(formMetadata, rs, tracker)) {
            List<Path> formAttachments = getFormAttachments(formMetadata);
            AtomicInteger attachmentSeq = new AtomicInteger(1);
            int totalAttachments = formAttachments.size();
            formAttachments.forEach(attachment ->
                pushFormAttachment(formMetadata.getKey(), attachment, rs, tracker, attachmentSeq.getAndIncrement(), totalAttachments)
            );
          }
        })
        .thenRun(rs -> {
          List<SubmissionMetadata> submissions = submissionMetadataPort.sortedSubmissions(formMetadata.getKey()).collect(toList());
          AtomicInteger submissionNumber = new AtomicInteger(1);
          int totalSubmissions = submissions.size();
          if (submissions.isEmpty())
            tracker.trackNoSubmissions();
          submissions.parallelStream().forEach(submissionMetadata -> {
            int currentSubmissionNumber = submissionNumber.getAndIncrement();
            boolean submissionSent = pushSubmission(
                submissionMetadata,
                rs,
                tracker,
                currentSubmissionNumber,
                totalSubmissions
            );
            if (submissionSent) {
              List<Path> submissionAttachments = getSubmissionAttachments(submissionMetadata.getSubmissionFile());
              AtomicInteger attachmentSeq = new AtomicInteger(1);
              int totalAttachments = submissionAttachments.size();
              submissionAttachments.parallelStream().forEach(attachment ->
                  pushSubmissionAttachment(
                      submissionMetadata.getKey(),
                      attachment,
                      rs,
                      tracker,
                      currentSubmissionNumber,
                      totalSubmissions,
                      attachmentSeq.getAndIncrement(),
                      totalAttachments
                  )
              );
            }
          });
        })
        .thenRun(rs -> {
          tracker.trackEnd();
          EventBus.publish(new PushEvent.Success());
        });
  }

  private List<Path> getSubmissionAttachments(Path submissionFile) {
    return list(submissionFile.getParent())
        .filter(p -> !p.getFileName().toString().equals("submission.xml"))
        .collect(toList());
  }

  boolean checkFormExists(FormMetadata formMetadata, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Check if form exists in Central");
      return false;
    }

    Response<Boolean> response = http.execute(server.getFormExistsRequest(formMetadata.getKey().getId(), token));
    if (!response.isSuccess()) {
      tracker.trackErrorCheckingForm(response);
      return false;
    }

    boolean exists = response.get();
    if (exists)
      tracker.trackFormAlreadyExists();
    return exists;
  }

  boolean pushForm(FormMetadata formMetadata, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return false;
    }

    tracker.trackStartSendingForm();
    var response = http.execute(server.getPushFormRequest(formMetadata.getFormFile(), token));

    if (response.isSuccess()) {
      tracker.trackEndSendingForm();
      return true;
    }

    if (response.getStatusCode() == 409) {
      tracker.trackFormAlreadyExists();
      return true;
    }

    tracker.trackErrorSendingForm(response);
    return false;
  }

  void pushFormAttachment(FormKey formKey, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form attachment " + attachment.getFileName());
      return;
    }

    tracker.trackStartSendingFormAttachment(attachmentNumber, totalAttachments);
    var response = http.execute(server.getPushFormAttachmentRequest(formKey.getId(), attachment, token));
    if (response.isSuccess())
      tracker.trackEndSendingFormAttachment(attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackFormAttachmentAlreadyExists(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  boolean pushSubmission(SubmissionMetadata submissionMetadata, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submissions " + submissionNumber + " of " + totalSubmissions);
      return false;
    }

    tracker.trackStartSendingSubmission(submissionNumber, totalSubmissions);
    var response = http.execute(server.getPushSubmissionRequest(token, submissionMetadata.getKey().getFormId(), submissionMetadata.getSubmissionFile()));

    if (response.isSuccess()) {
      tracker.trackEndSendingSubmission(submissionNumber, totalSubmissions);
      return true;
    }

    if (response.getStatusCode() == 409) {
      tracker.trackSubmissionAlreadyExists(submissionNumber, totalSubmissions);
      return true;
    }

    tracker.trackErrorSendingSubmission(submissionNumber, totalSubmissions, response);
    return false;
  }

  void pushSubmissionAttachment(SubmissionKey submissionKey, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submission attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    tracker.trackStartSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    var response = http.execute(server.getPushSubmissionAttachmentRequest(token, submissionKey.getFormId(), submissionKey.getInstanceId(), attachment));
    if (response.isSuccess())
      tracker.trackEndSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackSubmissionAttachmentAlreadyExists(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }

  private List<Path> getFormAttachments(FormMetadata formMetadata) {
    Path formMediaDir = formMetadata.getFormMediaDir();
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.emptyList();
  }
}
