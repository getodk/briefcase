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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.readAllBytes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.export.SubmissionMetaData;
import org.opendatakit.briefcase.operations.export.XmlElement;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.reused.api.Triple;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PushToCentral {

  private final Http http;
  private final CentralServer server;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PushToCentral(Http http, CentralServer server, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
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
        .thenSupply(rs -> checkFormExists(formMetadata, rs, tracker))
        .thenAccept(((rs, formExists) -> {
          if (!formExists) {
            boolean formSent = pushForm(formMetadata, rs, tracker);
            if (formSent) {
              List<Path> formAttachments = getFormAttachments(formMetadata);
              AtomicInteger attachmentSeq = new AtomicInteger(1);
              int totalAttachments = formAttachments.size();
              formAttachments.forEach(attachment ->
                  pushFormAttachment(formMetadata, attachment, rs, tracker, attachmentSeq.getAndIncrement(), totalAttachments)
              );
            }
          }
        }))
        .thenSupply(__ -> getSubmissions(formMetadata))
        .thenAccept((rs, submissions) -> {
          AtomicInteger submissionNumber = new AtomicInteger(1);
          int totalSubmissions = submissions.size();
          if (submissions.isEmpty())
            tracker.trackNoSubmissions();
          submissions.parallelStream()
              .map(submission -> {
                XmlElement root = XmlElement.from(new String(readAllBytes(submission)));
                SubmissionMetaData metaData = new SubmissionMetaData(root);
                metaData.getInstanceId();
                return Triple.of(submission, submissionNumber.getAndIncrement(), metaData.getInstanceId());
              })
              .peek(triple -> {
                if (triple.get3().isEmpty())
                  tracker.trackNoInstanceId(triple.get2(), totalSubmissions);
              })
              .filter(triple -> triple.get3().isPresent())
              .forEach(triple -> {
                Path submission = triple.get1();
                int currentSubmissionNumber = triple.get2();
                String instanceId = triple.get3().get();
                boolean submissionSent = pushSubmission(formMetadata, submission, rs, tracker, currentSubmissionNumber, totalSubmissions);
                if (submissionSent) {
                  List<Path> submissionAttachments = getSubmissionAttachments(submission);
                  AtomicInteger attachmentSeq = new AtomicInteger(1);
                  int totalAttachments = submissionAttachments.size();
                  submissionAttachments.parallelStream().forEach(attachment ->
                      pushSubmissionAttachment(formMetadata, instanceId, attachment, rs, tracker, currentSubmissionNumber, totalSubmissions, attachmentSeq.getAndIncrement(), totalAttachments)
                  );
                }
              });
          tracker.trackEnd();
        })
        .thenRun(rs -> EventBus.publish(new PushEvent.Success()));
  }

  private List<Path> getSubmissionAttachments(Path formFile) {
    Path submissionDir = formFile.getParent();
    return list(submissionDir)
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

  void pushFormAttachment(FormMetadata formMetadata, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form attachment " + attachment.getFileName());
      return;
    }

    tracker.trackStartSendingFormAttachment(attachmentNumber, totalAttachments);
    var response = http.execute(server.getPushFormAttachmentRequest(formMetadata.getKey().getId(), attachment, token));
    if (response.isSuccess())
      tracker.trackEndSendingFormAttachment(attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackFormAttachmentAlreadyExists(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  boolean pushSubmission(FormMetadata formMetadata, Path submissionFile, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submissions " + submissionNumber + " of " + totalSubmissions);
      return false;
    }

    tracker.trackStartSendingSubmission(submissionNumber, totalSubmissions);
    var response = http.execute(server.getPushSubmissionRequest(token, formMetadata.getKey().getId(), submissionFile));

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

  void pushSubmissionAttachment(FormMetadata formMetadata, String instanceId, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submission attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    tracker.trackStartSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    var response = http.execute(server.getPushSubmissionAttachmentRequest(token, formMetadata.getKey().getId(), instanceId, attachment));
    if (response.isSuccess())
      tracker.trackEndSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackSubmissionAttachmentAlreadyExists(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }

  private List<Path> getSubmissions(FormMetadata formMetadata) {
    Path submissionsDir = formMetadata.getSubmissionsDir();
    if (!exists(submissionsDir))
      return emptyList();
    return list(submissionsDir)
        .filter(UncheckedFiles::isInstanceDir)
        .map(submissionDir -> submissionDir.resolve("submission.xml"))
        .collect(toList());
  }

  private List<Path> getFormAttachments(FormMetadata formMetadata) {
    Path formMediaDir = formMetadata.getFormMediaDir();
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.emptyList();
  }
}
