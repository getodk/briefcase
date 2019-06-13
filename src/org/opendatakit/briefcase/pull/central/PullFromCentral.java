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
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromCentral {
  public static final Logger log = LoggerFactory.getLogger(PullFromCentral.class);
  private final Http http;
  private final CentralServer server;
  private final Path briefcaseDir;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PullFromCentral(Http http, CentralServer server, Path briefcaseDir, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.token = token;
    this.onEventCallback = onEventCallback;
  }

  /**
   * Pulls a form completely, writing the form file, form attachments,
   * submission files and their attachments to the local filesystem
   * under the Briefcase Storage directory.
   */
  public Job<FormStatus> pull(FormStatus form) {
    createDirectories(form.getFormDir(briefcaseDir));
    createDirectories(form.getFormMediaDir(briefcaseDir));

    PullFromCentralTracker tracker = new PullFromCentralTracker(form, onEventCallback);

    return allOf(
        supply(runnerStatus -> getSubmissions(form, token, runnerStatus, tracker)),
        run(runnerStatus -> downloadForm(form, token, runnerStatus, tracker))
            .thenSupply(runnerStatus -> getFormAttachments(form, token, runnerStatus, tracker))
            .thenAccept((runnerStatus, attachments) -> attachments.forEach(attachment -> downloadFormAttachment(form, attachment, token, runnerStatus, tracker)))
    ).thenApply((runnerStatus, pair) -> withDb(form.getFormDir(briefcaseDir), db -> {
      pair.getLeft().stream()
          .filter(instanceId -> db.hasRecordedInstance(instanceId) == null)
          .forEach(instanceId -> {
            downloadSubmission(form, instanceId, token, runnerStatus, tracker);
            getSubmissionAttachments(form, instanceId, token, runnerStatus, tracker).forEach(attachment ->
                downloadSubmissionAttachment(form, instanceId, attachment, token, runnerStatus, tracker)
            );
            db.putRecordedInstanceDirectory(instanceId, form.getSubmissionDir(briefcaseDir, instanceId).toFile());
          });
      return form;
    }));
  }

  void downloadForm(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return;
    }

    Path formFile = form.getFormFile(briefcaseDir);
    createDirectories(formFile.getParent());

    Response response = http.execute(server.getDownloadFormRequest(form.getFormId(), formFile, token));
    if (response.isSuccess())
      tracker.trackFormDownloaded();
    else
      tracker.trackError("Error downloading form", response);
  }

  List<CentralAttachment> getFormAttachments(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    Response<List<CentralAttachment>> response = http.execute(server.getFormAttachmentListRequest(form.getFormId(), token));
    if (!response.isSuccess()) {
      tracker.trackError("Error getting form attachments", response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    List<CentralAttachment> existingAttachments = attachments.stream().filter(CentralAttachment::exists).collect(toList());
    if (existingAttachments.size() != attachments.size())
      tracker.trackError("The remote server is missing some form attachments");

    tracker.trackFormAttachments(existingAttachments.size());
    return existingAttachments;
  }

  void downloadFormAttachment(FormStatus form, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getName());
      return;
    }


    Path targetFile = form.getFormMediaFile(briefcaseDir, attachment.getName());
    createDirectories(targetFile.getParent());

    Response response = http.execute(server.getDownloadFormAttachmentRequest(form.getFormId(), attachment, targetFile, token));
    if (response.isSuccess())
      tracker.formAttachmentDownloaded(attachment);
    else
      tracker.trackError("Error downloading form attachment " + attachment.getName(), response);
  }

  List<String> getSubmissions(FormStatus form, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions");
      return emptyList();
    }

    Response<List<String>> response = http.execute(server.getInstanceIdListRequest(form.getFormId(), token));
    if (!response.isSuccess()) {
      tracker.trackError("Error getting submissions", response);
      return emptyList();
    }

    List<String> instanceIds = response.get();
    tracker.trackTotalSubmissions(instanceIds.size());
    return instanceIds;
  }

  void downloadSubmission(FormStatus form, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + instanceId);
      return;
    }

    createDirectories(form.getSubmissionDir(briefcaseDir, instanceId));
    Response response = http.execute(server.getDownloadSubmissionRequest(form.getFormId(), instanceId, form.getSubmissionFile(briefcaseDir, instanceId), token));
    if (response.isSuccess())
      tracker.trackSubmission();
    else
      tracker.trackError("Error downloading submission " + instanceId, response);
  }

  List<CentralAttachment> getSubmissionAttachments(FormStatus form, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submission attachments of " + instanceId);
      return emptyList();
    }

    Response<List<CentralAttachment>> response = http.execute(server.getSubmissionAttachmentListRequest(form.getFormId(), instanceId, token));
    if (!response.isSuccess()) {
      tracker.trackError("Error getting submission attachments of " + instanceId, response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    tracker.trackSubmissionAttachments(instanceId, attachments.size());
    return attachments;
  }

  void downloadSubmissionAttachment(FormStatus form, String instanceId, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission attachment " + attachment.getName() + " of " + instanceId);
      return;
    }

    Path targetFile = form.getSubmissionMediaFile(briefcaseDir, instanceId, attachment.getName());
    createDirectories(targetFile.getParent());

    Response response = http.execute(server.getDownloadSubmissionAttachmentRequest(form.getFormId(), instanceId, attachment, targetFile, token));
    if (response.isSuccess())
      tracker.submissionAttachmentDownloaded(instanceId, attachment);
    else
      tracker.trackError("Error downloading attachment " + attachment.getName() + " of submission " + instanceId, response);
  }
}
