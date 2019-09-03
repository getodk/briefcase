/*
 * Copyright (C) 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * workspace.http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.operations.transfer.pull.central;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.job.Job.allOf;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.job.Job.supply;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands.insert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.api.Triple;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PullFromCentral {
  private final Workspace workspace;
  private final CentralServer server;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PullFromCentral(Workspace workspace, CentralServer server, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.workspace = workspace;
    this.server = server;
    this.token = token;
    this.onEventCallback = onEventCallback;
  }

  /**
   * Pulls a form completely, writing the form file, form attachments,
   * submission files and their attachments to the local filesystem
   * under the Briefcase Storage directory.
   */
  public Job<Void> pull(FormMetadata formMetadata) {
    PullFromCentralTracker tracker = new PullFromCentralTracker(formMetadata.getKey(), onEventCallback);

    return run(rs -> tracker.trackStart())
        .thenRun(allOf(
            supply(runnerStatus -> getSubmissionIds(formMetadata, token, runnerStatus, tracker)),
            run(runnerStatus -> {
              downloadForm(formMetadata, token, runnerStatus, tracker);
              List<CentralAttachment> attachments = getFormAttachments(formMetadata, token, runnerStatus, tracker);
              int totalAttachments = attachments.size();
              AtomicInteger attachmentNumber = new AtomicInteger(1);
              attachments.parallelStream().forEach(attachment ->
                  downloadFormAttachment(formMetadata, attachment, token, runnerStatus, tracker, attachmentNumber.getAndIncrement(), totalAttachments)
              );
            })
        ))
        .thenAccept((runnerStatus, pair) -> {
          List<String> submissions = pair.getLeft();
          int totalSubmissions = submissions.size();
          AtomicInteger submissionNumber = new AtomicInteger(1);

          if (submissions.isEmpty())
            tracker.trackNoSubmissions();

          submissions.stream()
              .map(instanceId -> Triple.of(submissionNumber.getAndIncrement(), instanceId, workspace.submissionMetadata.hasBeenAlreadyPulled(formMetadata.getKey().getId(), instanceId)))
              .peek(triple -> {
                if (triple.get3())
                  tracker.trackSubmissionAlreadyDownloaded(triple.get1(), totalSubmissions);
              })
              .filter(not(Triple::get3))
              .forEach(triple -> {
                int currentSubmissionNumber = triple.get1();
                String instanceId = triple.get2();
                downloadSubmission(formMetadata, instanceId, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions);
                List<CentralAttachment> attachments = getSubmissionAttachments(formMetadata, instanceId, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions);
                int totalAttachments = attachments.size();
                AtomicInteger attachmentNumber = new AtomicInteger(1);
                attachments.forEach(attachment ->
                    downloadSubmissionAttachment(formMetadata, instanceId, attachment, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions, attachmentNumber.getAndIncrement(), totalAttachments)
                );
                SubmissionMetadata submissionMetadata = SubmissionMetadata.from(
                    formMetadata.getSubmissionFile(instanceId),
                    instanceId,
                    attachments.stream().map(CentralAttachment::getName).map(Paths::get).collect(toList())
                );
                workspace.submissionMetadata.execute(insert(submissionMetadata));
              });
          tracker.trackEnd();

          workspace.formMetadata.execute(upsert(formMetadata));
          EventBus.publish(PullEvent.Success.of(formMetadata.getKey(), server));
        });
  }

  void downloadForm(FormMetadata formMetadata, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return;
    }

    createDirectories(formMetadata.getFormDir());

    tracker.trackStartDownloadingForm();
    Response response = workspace.http.execute(server.getDownloadFormRequest(formMetadata.getKey().getId(), formMetadata.getFormFile(), token));
    if (response.isSuccess())
      tracker.trackEndDownloadingForm();
    else
      tracker.trackErrorDownloadingForm(response);
  }

  List<CentralAttachment> getFormAttachments(FormMetadata formMetadata, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    tracker.trackStartGettingFormAttachments();
    Response<List<CentralAttachment>> response = workspace.http.execute(server.getFormAttachmentListRequest(formMetadata.getKey().getId(), token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingFormAttachments(response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    List<CentralAttachment> existingAttachments = attachments.stream().filter(CentralAttachment::exists).collect(toList());
    tracker.trackEndGettingFormAttachments();
    tracker.trackNonExistingFormAttachments(existingAttachments.size(), attachments.size());
    return existingAttachments;
  }

  void downloadFormAttachment(FormMetadata formMetadata, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getName());
      return;
    }

    Path targetFile = formMetadata.getFormMediaFile(attachment.getName());
    createDirectories(targetFile.getParent());

    tracker.trackStartDownloadingFormAttachment(attachmentNumber, totalAttachments);
    Response response = workspace.http.execute(server.getDownloadFormAttachmentRequest(formMetadata.getKey().getId(), attachment, targetFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingFormAttachment(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  List<String> getSubmissionIds(FormMetadata formMetadata, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions");
      return emptyList();
    }

    tracker.trackStartGettingSubmissionIds();
    Response<List<String>> response = workspace.http.execute(server.getInstanceIdListRequest(formMetadata.getKey().getId(), token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionIds(response);
      return emptyList();
    }

    List<String> instanceIds = response.get();
    tracker.trackEndGettingSubmissionIds();
    return instanceIds;
  }

  void downloadSubmission(FormMetadata formMetadata, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + instanceId);
      return;
    }

    createDirectories(formMetadata.getSubmissionDir(instanceId));

    tracker.trackStartDownloadingSubmission(submissionNumber, totalSubmissions);
    Response response = workspace.http.execute(server.getDownloadSubmissionRequest(formMetadata.getKey().getId(), instanceId, formMetadata.getSubmissionFile(instanceId), token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmission(submissionNumber, totalSubmissions);
    else
      tracker.trackErrorDownloadingSubmission(submissionNumber, totalSubmissions, response);
  }

  List<CentralAttachment> getSubmissionAttachments(FormMetadata formMetadata, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submission attachments of " + instanceId);
      return emptyList();
    }

    tracker.trackStartGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    Response<List<CentralAttachment>> response = workspace.http.execute(server.getSubmissionAttachmentListRequest(formMetadata.getKey().getId(), instanceId, token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionAttachments(submissionNumber, totalSubmissions, response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    tracker.trackEndGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    return attachments;
  }

  void downloadSubmissionAttachment(FormMetadata formMetadata, String instanceId, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission attachment " + attachment.getName() + " of " + instanceId);
      return;
    }

    Path targetFile = formMetadata.getSubmissionMediaFile(instanceId, attachment.getName());
    createDirectories(targetFile.getParent());

    tracker.trackStartDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = workspace.http.execute(server.getDownloadSubmissionAttachmentRequest(formMetadata.getKey().getId(), instanceId, attachment, targetFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }
}
