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

package org.opendatakit.briefcase.pull.central;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.job.Job.allOf;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.job.Job.supply;
import static org.opendatakit.briefcase.util.DatabaseUtils.withDb;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadataCommands;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.Triple;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.transfer.CentralServer;

public class PullFromCentral {
  private final Http http;
  private final CentralServer server;
  private final Path briefcaseDir;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadataPort formMetadataPort;

  public PullFromCentral(Http http, CentralServer server, Path briefcaseDir, String token, Consumer<FormStatusEvent> onEventCallback, FormMetadataPort formMetadataPort) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.token = token;
    this.onEventCallback = onEventCallback;
    this.formMetadataPort = formMetadataPort;
  }

  /**
   * Pulls a form completely, writing the form file, form attachments,
   * submission files and their attachments to the local filesystem
   * under the Briefcase Storage directory.
   */
  public Job<Void> pull(FormStatus form) {
    FormKey key = FormKey.from(form);

    PullFromCentralTracker tracker = new PullFromCentralTracker(form, onEventCallback);

    return run(rs -> tracker.trackStart())
        .thenRun(allOf(
            supply(runnerStatus -> getSubmissionIds(form, token, runnerStatus, tracker)),
            run(runnerStatus -> {
              downloadForm(form, token, runnerStatus, tracker);
              List<CentralAttachment> attachments = getFormAttachments(form, token, runnerStatus, tracker);
              int totalAttachments = attachments.size();
              AtomicInteger attachmentNumber = new AtomicInteger(1);
              attachments.parallelStream().forEach(attachment ->
                  downloadFormAttachment(form, attachment, token, runnerStatus, tracker, attachmentNumber.getAndIncrement(), totalAttachments)
              );
            })
        ))
        .thenAccept((runnerStatus, pair) -> withDb(form.getFormDir(), db -> {
          List<String> submissions = pair.getLeft();
          int totalSubmissions = submissions.size();
          AtomicInteger submissionNumber = new AtomicInteger(1);

          if (submissions.isEmpty())
            tracker.trackNoSubmissions();

          submissions.stream()
              .map(instanceId -> Triple.of(submissionNumber.getAndIncrement(), instanceId, db.hasRecordedInstance(instanceId) == null))
              .peek(triple -> {
                if (!triple.get3())
                  tracker.trackSubmissionAlreadyDownloaded(triple.get1(), totalSubmissions);
              })
              .filter(Triple::get3)
              .forEach(triple -> {
                int currentSubmissionNumber = triple.get1();
                String instanceId = triple.get2();
                downloadSubmission(form, instanceId, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions);
                List<CentralAttachment> attachments = getSubmissionAttachments(form, instanceId, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions);
                int totalAttachments = attachments.size();
                AtomicInteger attachmentNumber = new AtomicInteger(1);
                attachments.forEach(attachment ->
                    downloadSubmissionAttachment(form, instanceId, attachment, token, runnerStatus, tracker, currentSubmissionNumber, totalSubmissions, attachmentNumber.getAndIncrement(), totalAttachments)
                );
                db.putRecordedInstanceDirectory(instanceId, form.getSubmissionDir(instanceId).toFile());
              });
          tracker.trackEnd();

          formMetadataPort.execute(FormMetadataCommands.upsert(key, form.getFormFile()));
          EventBus.publish(PullEvent.Success.of(form, server));
        }));
  }

  void downloadForm(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return;
    }

    Path formFile = form.getFormFile();
    createDirectories(formFile.getParent());

    tracker.trackStartDownloadingForm();
    Response response = http.execute(server.getDownloadFormRequest(form.getFormId(), formFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingForm();
    else
      tracker.trackErrorDownloadingForm(response);
  }

  List<CentralAttachment> getFormAttachments(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    tracker.trackStartGettingFormAttachments();
    Response<List<CentralAttachment>> response = http.execute(server.getFormAttachmentListRequest(form.getFormId(), token));
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

  void downloadFormAttachment(FormStatus form, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getName());
      return;
    }

    Path targetFile = form.getFormMediaFile(attachment.getName());
    createDirectories(targetFile.getParent());

    tracker.trackStartDownloadingFormAttachment(attachmentNumber, totalAttachments);
    Response response = http.execute(server.getDownloadFormAttachmentRequest(form.getFormId(), attachment, targetFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingFormAttachment(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  List<String> getSubmissionIds(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions");
      return emptyList();
    }

    tracker.trackStartGettingSubmissionIds();
    Response<List<String>> response = http.execute(server.getInstanceIdListRequest(form.getFormId(), token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionIds(response);
      return emptyList();
    }

    List<String> instanceIds = response.get();
    tracker.trackEndGettingSubmissionIds();
    return instanceIds;
  }

  void downloadSubmission(FormStatus form, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + instanceId);
      return;
    }

    createDirectories(form.getSubmissionDir(instanceId));

    tracker.trackStartDownloadingSubmission(submissionNumber, totalSubmissions);
    Response response = http.execute(server.getDownloadSubmissionRequest(form.getFormId(), instanceId, form.getSubmissionFile(instanceId), token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmission(submissionNumber, totalSubmissions);
    else
      tracker.trackErrorDownloadingSubmission(submissionNumber, totalSubmissions, response);
  }

  List<CentralAttachment> getSubmissionAttachments(FormStatus form, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submission attachments of " + instanceId);
      return emptyList();
    }

    tracker.trackStartGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    Response<List<CentralAttachment>> response = http.execute(server.getSubmissionAttachmentListRequest(form.getFormId(), instanceId, token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionAttachments(submissionNumber, totalSubmissions, response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    tracker.trackEndGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    return attachments;
  }

  void downloadSubmissionAttachment(FormStatus form, String instanceId, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission attachment " + attachment.getName() + " of " + instanceId);
      return;
    }

    Path targetFile = form.getSubmissionMediaFile(instanceId, attachment.getName());
    createDirectories(targetFile.getParent());

    tracker.trackStartDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = http.execute(server.getDownloadSubmissionAttachmentRequest(form.getFormId(), instanceId, attachment, targetFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }
}
