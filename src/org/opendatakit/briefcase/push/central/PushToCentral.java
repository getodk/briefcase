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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.export.SubmissionParser.parseSubmission;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.DateRange;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.export.Submission;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
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

    // Verify that the form is not encrypted
    Path formPath = form.getFormFile(briefcaseDir);
    FormDefinition formDef = FormDefinition.from(formPath);
    if (formDef.isFileEncryptedForm())
      throw new BriefcaseException("Encrypted form can't be pushed to ODK Central");

    return Job.supply(runnerStatus -> checkFormExists(form.getFormId(), runnerStatus, tracker))
        .thenAccept(((runnerStatus, formExists) -> {
          if (!formExists) {
            pushForm(formPath, runnerStatus, tracker);
            getFormAttachments(form).forEach(attachment -> pushFormAttachment(formDef.getFormId(), attachment, runnerStatus, tracker));
          }
        }))
        .thenSupply(__ -> getSubmissions(formDef))
        .thenAccept((runnerStatus, submissions) -> submissions.forEach(submission -> {
          pushSubmission(formDef.getFormId(), submission.getInstanceId(), submission.getPath(), runnerStatus, tracker);
          list(form.getSubmissionDir(briefcaseDir, submission.getInstanceId()))
              .filter(p -> !p.getFileName().toString().equals("submission.xml"))
              .collect(toList())
              .forEach(attachment ->
                  pushSubmissionAttachment(formDef.getFormId(), submission.getInstanceId(), attachment, runnerStatus, tracker)
              );
        }));
  }

  boolean checkFormExists(String formId, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Check if form exists in Central");
      return false;
    }

    Response<Boolean> response = http.execute(server.getFormExistsRequest(formId, token));
    if (!response.isSuccess()) {
      tracker.trackError("Error checking if form exists in Central", response);
      return false;
    }

    boolean exists = response.get();
    tracker.trackFormAlreadyExists(exists);
    return exists;
  }

  void pushForm(Path formFile, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return;
    }

    Response response = http.execute(server.getPushFormRequest(formFile, token));
    if (response.isSuccess())
      tracker.trackPushForm();
    else if (response.getStatusCode() == 409)
      tracker.trackFormAlreadyExists();
    else
      tracker.trackError("Failed to push form", response);
  }

  void pushFormAttachment(String formId, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form attachment " + attachment.getFileName());
      return;
    }

    Response response = http.execute(server.getPushFormAttachmentRequest(formId, attachment, token));
    if (response.isSuccess())
      tracker.trackPushFormAttachment(attachment);
    else if (response.getStatusCode() == 409)
      tracker.trackFormAttachmentAlreadyExists(attachment);
    else
      tracker.trackError("Failed to push form attachment " + attachment.getFileName(), response);
  }

  void pushSubmission(String formId, String instanceId, Path submissionFile, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push submission " + instanceId);
      return;
    }

    Response response = http.execute(server.getPushSubmissionRequest(token, formId, submissionFile));
    if (response.isSuccess())
      tracker.pushSubmission(instanceId);
    else if (response.getStatusCode() == 409)
      tracker.trackSubmissionAlreadyExists(instanceId);
    else
      tracker.trackError("Error pushing submission " + instanceId, response);
  }

  void pushSubmissionAttachment(String formId, String instanceId, Path attachment, RunnerStatus runnerStatus, PushToCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push attachment " + attachment.getFileName() + " of submission " + instanceId);
      return;
    }

    Response response = http.execute(server.getPushSubmissionAttachmentRequest(token, formId, instanceId, attachment));
    if (response.isSuccess())
      tracker.trackPushSubmissionAttachment(attachment, instanceId);
    else if (response.getStatusCode() == 409)
      tracker.trackSubmissionAttachmentAlreadyExists(attachment, instanceId);
    else
      tracker.trackError("Error pushing submission attachment " + attachment.getFileName(), response);
  }

  private Stream<Submission> getSubmissions(FormDefinition formDef) {
    return getListOfSubmissionFiles(formDef, DateRange.empty(), (path, message) -> { })
        .stream()
        .map(file -> parseSubmission(file, false, Optional.empty(), (path, message) -> { }))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private List<Path> getFormAttachments(FormStatus form) {
    Path formMediaDir = form.getFormMediaDir(briefcaseDir);
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.emptyList();
  }
}
