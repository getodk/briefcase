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
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands.insert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.api.Triple;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PullFromCentral {
  private final Container container;
  private final CentralServer server;
  private final String token;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PullFromCentral(Container container, CentralServer server, String token, Consumer<FormStatusEvent> onEventCallback) {
    this.container = container;
    this.server = server;
    this.token = token;
    this.onEventCallback = onEventCallback;
  }

  /**
   * Pulls a form completely, writing the form file, form attachments,
   * submission files and their attachments to the local filesystem
   * under the Briefcase Storage directory.
   */
  public Job<Void> pull(FormMetadata sourceFormMetadata, Path targetFormFile) {
    FormKey formKey = sourceFormMetadata.getKey();
    PullFromCentralTracker tracker = new PullFromCentralTracker(formKey, onEventCallback);
    String formId = formKey.getId();

    return run(rs -> {
      tracker.trackStart();
      FormMetadata targetFormMetadata = downloadForm(formId, targetFormFile, token, rs, tracker);
      if (targetFormMetadata == null)
        return;

      List<CentralAttachment> formAttachments = getFormAttachments(formId, token, rs, tracker);
      int totalFormAttachments = formAttachments.size();
      AtomicInteger formAttachmentNumber = new AtomicInteger(1);
      formAttachments.parallelStream().forEach(attachment ->
          downloadFormAttachment(formId, targetFormMetadata.getFormMediaFile(attachment.getName()), attachment, token, rs, tracker, formAttachmentNumber.getAndIncrement(), totalFormAttachments)
      );

      List<String> submissions = getSubmissionIds(formId, token, rs, tracker);
      int totalSubmissions = submissions.size();
      AtomicInteger submissionNumber = new AtomicInteger(1);

      if (submissions.isEmpty())
        tracker.trackNoSubmissions();

      submissions.stream()
          .map(instanceId -> Triple.of(submissionNumber.getAndIncrement(), instanceId, container.submissionMetadata.hasBeenAlreadyPulled(formId, instanceId)))
          .peek(triple -> {
            if (triple.get3())
              tracker.trackSubmissionAlreadyDownloaded(triple.get1(), totalSubmissions);
          })
          .filter(not(Triple::get3))
          .forEach(triple -> {
            int currentSubmissionNumber = triple.get1();
            String instanceId = triple.get2();
            downloadSubmission(formId, instanceId, targetFormMetadata.getSubmissionFile(instanceId), token, rs, tracker, currentSubmissionNumber, totalSubmissions);
            List<CentralAttachment> submissionAttachments = getSubmissionAttachments(formId, instanceId, token, rs, tracker, currentSubmissionNumber, totalSubmissions);
            int totalSubmissionAttachments = submissionAttachments.size();
            AtomicInteger submissionAttachmentNumber = new AtomicInteger(1);
            submissionAttachments.forEach(attachment ->
                downloadSubmissionAttachment(formId, instanceId, attachment, targetFormMetadata.getSubmissionAttachmentFile(instanceId, attachment.getName()), token, rs, tracker, currentSubmissionNumber, totalSubmissions, submissionAttachmentNumber.getAndIncrement(), totalSubmissionAttachments)
            );
            SubmissionMetadata submissionMetadata = SubmissionMetadata.from(
                targetFormMetadata.getSubmissionFile(instanceId),
                instanceId,
                submissionAttachments.stream()
                    .filter(not(CentralAttachment::isEncryptedSubmissionFile))
                    .map(CentralAttachment::getName)
                    .map(Paths::get)
                    .collect(toList())
            );
            container.submissionMetadata.execute(insert(submissionMetadata));
          });
      tracker.trackEnd();

      container.formMetadata.execute(upsert(targetFormMetadata));
      EventBus.publish(PullEvent.Success.of(formKey, server));
    });
  }

  FormMetadata downloadForm(String formId, Path targetFormFile, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return null;
    }

    createDirectories(targetFormFile.getParent());

    tracker.trackStartDownloadingForm();
    Response response = container.http.execute(server.getDownloadFormRequest(formId, targetFormFile, token));
    if (response.isSuccess()) {
      tracker.trackEndDownloadingForm();
      return FormMetadata.from(targetFormFile);
    } else {
      tracker.trackErrorDownloadingForm(response);
      return null;
    }
  }

  List<CentralAttachment> getFormAttachments(String formId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    tracker.trackStartGettingFormAttachments();
    Response<List<CentralAttachment>> response = container.http.execute(server.getFormAttachmentListRequest(formId, token));
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

  void downloadFormAttachment(String formId, Path targetAttachmentFile, CentralAttachment attachment, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getName());
      return;
    }

    createDirectories(targetAttachmentFile.getParent());

    tracker.trackStartDownloadingFormAttachment(attachmentNumber, totalAttachments);
    Response response = container.http.execute(server.getDownloadFormAttachmentRequest(formId, attachment, targetAttachmentFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingFormAttachment(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  List<String> getSubmissionIds(String formId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions");
      return emptyList();
    }

    tracker.trackStartGettingSubmissionIds();
    Response<List<String>> response = container.http.execute(server.getInstanceIdListRequest(formId, token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionIds(response);
      return emptyList();
    }

    List<String> instanceIds = response.get();
    tracker.trackEndGettingSubmissionIds();
    return instanceIds;
  }

  void downloadSubmission(String formId, String instanceId, Path targetSubmissionFile, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + instanceId);
      return;
    }

    createDirectories(targetSubmissionFile.getParent());

    tracker.trackStartDownloadingSubmission(submissionNumber, totalSubmissions);
    Response response = container.http.execute(server.getDownloadSubmissionRequest(formId, instanceId, targetSubmissionFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmission(submissionNumber, totalSubmissions);
    else
      tracker.trackErrorDownloadingSubmission(submissionNumber, totalSubmissions, response);
  }

  List<CentralAttachment> getSubmissionAttachments(String formId, String instanceId, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submission attachments of " + instanceId);
      return emptyList();
    }

    tracker.trackStartGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    Response<List<CentralAttachment>> response = container.http.execute(server.getSubmissionAttachmentListRequest(formId, instanceId, token));
    if (!response.isSuccess()) {
      tracker.trackErrorGettingSubmissionAttachments(submissionNumber, totalSubmissions, response);
      return emptyList();
    }

    List<CentralAttachment> attachments = response.get();
    tracker.trackEndGettingSubmissionAttachments(submissionNumber, totalSubmissions);
    return attachments;
  }

  void downloadSubmissionAttachment(String formId, String instanceId, CentralAttachment attachment, Path targetAttachmentFile, String token, RunnerStatus runnerStatus, PullFromCentralTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission attachment " + attachment.getName() + " of " + instanceId);
      return;
    }

    createDirectories(targetAttachmentFile.getParent());

    tracker.trackStartDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = container.http.execute(server.getDownloadSubmissionAttachmentRequest(formId, instanceId, attachment, targetAttachmentFile, token));
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }
}
