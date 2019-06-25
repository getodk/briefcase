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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.size;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;

public class PushToAggregate {
  private static final int BYTES_IN_ONE_MEGABYTE = 1_048_576;
  private final Http http;
  private final AggregateServer server;
  private final Path briefcaseDir;
  private final boolean forceSendForm;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PushToAggregate(Http http, AggregateServer server, Path briefcaseDir, boolean forceSendForm, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.forceSendForm = forceSendForm;
    this.onEventCallback = onEventCallback;
  }

  /**
   * Pushes a form completely, sending the form file, form attachments,
   * submission files and their attachments to the Aggregate server.
   * <p>
   * By default, it won't push a form and form attachments if it's already
   * present in the server, which can be overriden with the {@link #forceSendForm}
   * field.
   */
  public Job<Void> push(FormStatus form) {
    PushToAggregateTracker tracker = new PushToAggregateTracker(form, onEventCallback);
    return Job
        .run(runnerStatus -> {
          tracker.trackStart();
          tracker.trackForceSendForm(forceSendForm);
        })
        .thenSupply(runnerStatus -> !forceSendForm && checkFormExists(form.getFormId(), runnerStatus, tracker))
        .thenAccept(((runnerStatus, formExists) -> {
          if (!formExists) {
            Path formFile = form.getFormFile(briefcaseDir);
            List<Path> allAttachments = getFormAttachments(form);
            if (allAttachments.isEmpty()) {
              pushFormAndAttachments(form, emptyList(), runnerStatus, tracker);
            } else {
              AtomicInteger partsSeq = new AtomicInteger(1);
              List<List<Path>> attachmentGroups = createGroupsOfMaxSize(formFile, allAttachments, 10);
              attachmentGroups.forEach(attachments ->
                  pushFormAndAttachments(form, attachments, runnerStatus, tracker, partsSeq.getAndIncrement(), attachmentGroups.size())
              );
            }
          }
        }))
        .thenSupply(__ -> getSubmissions(form))
        .thenAccept((runnerStatus, submissions) -> {
          AtomicInteger submissionsSeq = new AtomicInteger(1);
          int totalSubmissions = submissions.size();
          if (submissions.isEmpty())
            tracker.trackNoSubmissions();
          submissions.parallelStream().forEach(submission -> {
            List<Path> allAttachments = getSubmissionAttachments(submission);
            if (allAttachments.isEmpty()) {
              pushSubmissionAndAttachments(submission, emptyList(), runnerStatus, tracker, submissionsSeq.getAndIncrement(), totalSubmissions);
            } else {
              AtomicInteger partsSeq = new AtomicInteger(1);
              List<List<Path>> attachmentGroups = createGroupsOfMaxSize(submission, allAttachments, 10);
              attachmentGroups.forEach(attachment ->
                  pushSubmissionAndAttachments(submission, allAttachments, runnerStatus, tracker, submissionsSeq.getAndIncrement(), totalSubmissions, partsSeq.getAndIncrement(), attachmentGroups.size())
              );
            }
          });
          tracker.trackEnd();
        })
        .thenRun(rs -> EventBus.publish(new PushEvent.Success(form)));
  }

  private List<Path> getSubmissionAttachments(Path formFile) {
    Path submissionDir = formFile.getParent();
    return UncheckedFiles.list(submissionDir)
        .filter(p -> !p.getFileName().toString().equals("submission.xml"))
        .collect(toList());
  }

  static List<List<Path>> createGroupsOfMaxSize(Path baseDocument, List<Path> attachments, int sizeInMegabytes) {
    int maxSize = sizeInMegabytes * BYTES_IN_ONE_MEGABYTE;
    long formSize = size(baseDocument);
    List<List<Path>> groupsOfAttachments = new ArrayList<>();
    long currentSize = formSize;
    List<Path> currentList = new ArrayList<>();
    groupsOfAttachments.add(currentList);
    for (Path attachment : attachments) {
      long attachmentSize = size(attachment);
      if (currentSize + attachmentSize > maxSize) {
        currentList = new ArrayList<>();
        currentSize = formSize;
        groupsOfAttachments.add(currentList);
      }
      currentList.add(attachment);
      currentSize += attachmentSize;
    }
    return groupsOfAttachments;
  }

  boolean checkFormExists(String formId, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Check if form exists in Aggregate");
      return false;
    }

    Response<Boolean> response = http.execute(server.getFormExistsRequest(formId));
    if (!response.isSuccess()) {
      tracker.trackErrorCheckingForm(response);
      return false;
    }

    boolean exists = response.get();
    tracker.trackFormAlreadyExists(exists);
    return exists;
  }

  void pushFormAndAttachments(FormStatus form, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    pushFormAndAttachments(form, attachments, runnerStatus, tracker, 1, 1);
  }

  void pushFormAndAttachments(FormStatus form, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker, int part, int parts) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return;
    }

    tracker.trackStartSendingFormAndAttachments(part, parts);
    Response response = http.execute(server.getPushFormRequest(form.getFormFile(briefcaseDir), attachments));
    if (response.isSuccess())
      tracker.trackEndSendingFormAndAttachments(part, parts);
    else
      tracker.trackErrorSendingFormAndAttachments(part, parts, response);
  }

  void pushSubmissionAndAttachments(Path submissionFile, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker, int submissionNumber, int totalSubmissions) {
    pushSubmissionAndAttachments(submissionFile, attachments, runnerStatus, tracker, submissionNumber, totalSubmissions, 1, 1);
  }

  void pushSubmissionAndAttachments(Path submissionFile, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker, int submissionNumber, int totalSubmissions, int part, int parts) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Sending submissions " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    tracker.trackStartSendingSubmissionAndAttachments(submissionNumber, totalSubmissions, part, parts);
    Response<XmlElement> response = http.execute(server.getPushSubmissionRequest(
        submissionFile,
        attachments
    ));
    if (response.isSuccess())
      tracker.trackEndSendingSubmissionAndAttachments(submissionNumber, totalSubmissions, part, parts);
    else
      tracker.trackErrorSendingSubmissionAndAttachments(submissionNumber, totalSubmissions, part, parts, response);
  }

  private List<Path> getSubmissions(FormStatus form) {
    Path submissionsDir = form.getSubmissionsDir(briefcaseDir);
    if (!UncheckedFiles.exists(submissionsDir))
      return emptyList();
    return UncheckedFiles.list(submissionsDir)
        .filter(UncheckedFiles::isInstanceDir)
        .map(submissionDir -> submissionDir.resolve("submission.xml"))
        .collect(toList());
  }

  private List<Path> getFormAttachments(FormStatus form) {
    Path formMediaDir = form.getFormMediaDir(briefcaseDir);
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : emptyList();
  }
}
