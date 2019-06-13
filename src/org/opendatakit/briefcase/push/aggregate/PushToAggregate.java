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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.size;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.DateRange;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;

public class PushToAggregate {
  public static final int BYTES_IN_ONE_MEGABYTE = 1_048_576;
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
  public Job<FormStatus> push(FormStatus form) {
    PushToAggregateTracker tracker = new PushToAggregateTracker(form, onEventCallback);
    tracker.trackForceSendForm(forceSendForm);
    return Job.supply(runnerStatus -> !forceSendForm && checkFormExists(form.getFormId(), runnerStatus, tracker))
        .thenAccept(((runnerStatus, formExists) -> {
          if (!formExists) {
            Path formFile = form.getFormFile(briefcaseDir);
            List<Path> allAttachments = getFormAttachments(form);
            if (allAttachments.isEmpty()) {
              tracker.trackStartSendingForm(1);
              pushFormAndAttachments(form, Collections.emptyList(), runnerStatus, tracker);
              tracker.trackEndSendingForm();
            } else {
              List<List<Path>> groupsOfAttachments = createGroupsOfMaxSize(formFile, allAttachments, 10);
              tracker.trackStartSendingForm(groupsOfAttachments.size());
              AtomicInteger seq = new AtomicInteger(1);
              groupsOfAttachments.forEach((List<Path> attachments) -> {
                tracker.trackStartSendingFormPart(seq.getAndIncrement(), groupsOfAttachments.size());
                pushFormAndAttachments(form, attachments, runnerStatus, tracker);
                tracker.trackEndSendingFormPart(seq.get(), groupsOfAttachments.size());
              });
              tracker.trackEndSendingForm();
            }
          }
        }))
        .thenSupply(__ -> getSubmissions(form))
        .thenApply((runnerStatus, submissions) -> {
          submissions.forEach(submission -> {
            String instanceId = submission.getInstanceId().orElseThrow(BriefcaseException::new);
            List<Path> allAttachments = UncheckedFiles.list(form.getSubmissionDir(briefcaseDir, instanceId))
                .filter(p -> !p.getFileName().toString().equals("submission.xml"))
                .collect(toList());
            Path submissionFile = form.getSubmissionFile(briefcaseDir, instanceId);
            if (allAttachments.isEmpty()) {
              tracker.trackStartSendingSubmission(1, instanceId);
              pushSubmissionAndAttachments(submissionFile, instanceId, Collections.emptyList(), runnerStatus, tracker);
              tracker.trackEndSendingSubmission(instanceId);
            } else {
              List<List<Path>> groupsOfAttachments = createGroupsOfMaxSize(submissionFile, allAttachments, 10);
              tracker.trackStartSendingSubmission(groupsOfAttachments.size(), instanceId);
              AtomicInteger seq = new AtomicInteger(1);
              groupsOfAttachments.forEach(attachment -> {
                tracker.trackStartSendingSubmissionPart(seq.getAndIncrement(), groupsOfAttachments.size(), instanceId);
                pushSubmissionAndAttachments(submissionFile, instanceId, allAttachments, runnerStatus, tracker);
                tracker.trackEndSendingSubmissionPart(seq.get(), groupsOfAttachments.size(), instanceId);
              });
              tracker.trackEndSendingSubmission(instanceId);
            }
          });
          return form;
        });
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
      tracker.trackError("Error checking if form exists in Aggregate", response);
      return false;
    }

    boolean exists = response.get();
    tracker.trackFormAlreadyExists(exists);
    return exists;
  }

  void pushFormAndAttachments(FormStatus form, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return;
    }

    tracker.trackStartPushing();

    Response response = http.execute(server.getPushFormRequest(form.getFormFile(briefcaseDir), attachments));
    if (!response.isSuccess())
      tracker.trackError("Failed to push form", response);
  }

  void pushSubmissionAndAttachments(Path submissionFile, String instanceId, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push submission " + instanceId);
      return;
    }

    Response<XmlElement> response = http.execute(server.getPushSubmissionRequest(
        submissionFile,
        attachments
    ));
    if (!response.isSuccess())
      tracker.trackError("Failed to push form", response);
  }

  private Stream<SubmissionMetaData> getSubmissions(FormStatus form) {
    return getListOfSubmissionFiles(FormDefinition.from(form.getFormFile(briefcaseDir)), DateRange.empty(), (path, message) -> { })
        .stream()
        .map(file -> {
          String xml = new String(readAllBytes(file));
          XmlElement from = XmlElement.from(xml);
          return new SubmissionMetaData(from);
        });
  }

  private List<Path> getFormAttachments(FormStatus form) {
    Path formMediaDir = form.getFormMediaDir(briefcaseDir);
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.emptyList();
  }
}
