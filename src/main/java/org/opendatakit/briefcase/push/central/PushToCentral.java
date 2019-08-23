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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.Triple;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.CentralServer;

public class PushToCentral {

  private final Http http;
  private final CentralServer server;
  private final Path briefcaseDir;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PushToCentral(Http http, CentralServer server, Path briefcaseDir, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
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
  public Job<Void> push(FormStatus form) {
    PushToCentralTracker tracker = new PushToCentralTracker(form, onEventCallback);

    Job<Void> startTrackingJob = Job.run(runnerStatus -> tracker.trackStart());

    // Verify that the form is not encrypted
    Path formFile = form.getFormFile();
    try {
      if (FormDefinition.from(formFile).isFileEncryptedForm())
        return startTrackingJob.thenRun(rs -> {
          tracker.trackEncryptedForm();
          tracker.trackEnd();
        });
    } catch (RuntimeException e) {
      return startTrackingJob.thenRun(rs -> {
        tracker.trackCannotDetermineEncryption(e);
        tracker.trackEnd();
      });
    }

    return startTrackingJob
        .thenSupply(rs -> checkFormExists(form.getFormId(), rs, tracker))
        .thenAccept(((rs, formExists) -> {
          if (!formExists) {
            boolean formSent = pushForm(formFile, rs, tracker);
            if (formSent) {
              List<Path> formAttachments = getFormAttachments(form);
              AtomicInteger attachmentSeq = new AtomicInteger(1);
              int totalAttachments = formAttachments.size();
              formAttachments.forEach(attachment ->
                  pushFormAttachment(form.getFormId(), attachment, rs, tracker, attachmentSeq.getAndIncrement(), totalAttachments)
              );
            }
          }
        }))
        .thenSupply(__ -> getSubmissions(form))
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
                if (!triple.get3().isPresent())
                  tracker.trackNoInstanceId(triple.get2(), totalSubmissions);
              })
              .filter(triple -> triple.get3().isPresent())
              .forEach(triple -> {
                Path submission = triple.get1();
                int currentSubmissionNumber = triple.get2();
                String instanceId = triple.get3().get();
                boolean submissionSent = pushSubmission(form.getFormId(), submission, rs, tracker, currentSubmissionNumber, totalSubmissions);
                if (submissionSent) {
                  List<Path> submissionAttachments = getSubmissionAttachments(submission);
                  AtomicInteger attachmentSeq = new AtomicInteger(1);
                  int totalAttachments = submissionAttachments.size();
                  submissionAttachments.parallelStream().forEach(attachment ->
                      pushSubmissionAttachment(form.getFormId(), instanceId, attachment, rs, tracker, currentSubmissionNumber, totalSubmissions, attachmentSeq.getAndIncrement(), totalAttachments)
                  );
                }
              });
          tracker.trackEnd();
        })
        .thenRun(rs -> EventBus.publish(new PushEvent.Success(form)));
  }

  private List<Path> getSubmissionAttachments(Path formFile) {
    Path submissionDir = formFile.getParent();
    return list(submissionDir)
        .filter(p -> !p.getFileName().toString().equals("submission.xml"))
        .collect(toList());
  }

  boolean checkFormExists(String formId, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Check if form exists in Central");
      return false;
    }

    Response<Boolean> response = http.execute(server.getFormExistsRequest(formId, token));
    if (!response.isSuccess()) {
      tracker.trackErrorCheckingForm(response);
      return false;
    }

    boolean exists = response.get();
    if (exists)
      tracker.trackFormAlreadyExists();
    return exists;
  }

  boolean pushForm(Path formFile, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return false;
    }

    tracker.trackStartSendingForm();
    Response response = http.execute(server.getPushFormRequest(formFile, token));

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

  void pushFormAttachment(String formId, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form attachment " + attachment.getFileName());
      return;
    }

    tracker.trackStartSendingFormAttachment(attachmentNumber, totalAttachments);
    Response response = http.execute(server.getPushFormAttachmentRequest(formId, attachment, token));
    if (response.isSuccess())
      tracker.trackEndSendingFormAttachment(attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackFormAttachmentAlreadyExists(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  boolean pushSubmission(String formId, Path submissionFile, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submissions " + submissionNumber + " of " + totalSubmissions);
      return false;
    }

    tracker.trackStartSendingSubmission(submissionNumber, totalSubmissions);
    Response response = http.execute(server.getPushSubmissionRequest(token, formId, submissionFile));

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

  void pushSubmissionAttachment(String formId, String instanceId, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submission attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    tracker.trackStartSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = http.execute(server.getPushSubmissionAttachmentRequest(token, formId, instanceId, attachment));
    if (response.isSuccess())
      tracker.trackEndSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else if (response.getStatusCode() == 409)
      tracker.trackSubmissionAttachmentAlreadyExists(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorSendingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }

  private List<Path> getSubmissions(FormStatus form) {
    Path submissionsDir = form.getSubmissionsDir();
    if (!exists(submissionsDir))
      return emptyList();
    return list(submissionsDir)
        .filter(UncheckedFiles::isInstanceDir)
        .map(submissionDir -> submissionDir.resolve("submission.xml"))
        .collect(toList());
  }

  private List<Path> getFormAttachments(FormStatus form) {
    Path formMediaDir = form.getFormMediaDir();
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.emptyList();
  }
}
