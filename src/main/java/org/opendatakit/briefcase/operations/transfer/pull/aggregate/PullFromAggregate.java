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

package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.write;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands.insert;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.api.Triple;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;

public class PullFromAggregate {
  private final Workspace workspace;
  private final AggregateServer server;
  private final boolean includeIncomplete;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PullFromAggregate(Workspace workspace, AggregateServer server, boolean includeIncomplete, Consumer<FormStatusEvent> onEventCallback) {
    this.workspace = workspace;
    this.server = server;
    this.includeIncomplete = includeIncomplete;
    this.onEventCallback = onEventCallback;
  }

  static Optional<Cursor> getLastCursor(List<InstanceIdBatch> batches) {
    return batches.stream()
        .map(InstanceIdBatch::getCursor)
        .reduce(maxBy(Cursor::compareTo));
  }

  /**
   * Pulls a form completely, writing the form file, form attachments,
   * submission files and their attachments to the local filesystem
   * under the Briefcase Storage directory.
   * <p>
   * A {@link Cursor} can be provided to define the starting point to
   * download the form's submissions.
   * <p>
   * Returns a Job that will produce a pull operation result.
   */
  public Job<Void> pull(FormMetadata sourceFormMetadata, Path targetFormFile, Optional<Cursor> lastCursor) {
    FormKey formKey = sourceFormMetadata.getKey();
    PullFromAggregateTracker tracker = new PullFromAggregateTracker(formKey, onEventCallback);
    String formId = formKey.getId();

    // Download the form and attachments, and get the submissions list
    return run(rs -> {
      tracker.trackStart();
      Pair<FormMetadata, SubmissionKeyGenerator> downloadResult = downloadForm(sourceFormMetadata, targetFormFile, rs, tracker);
      if (downloadResult == null)
        return;

      FormMetadata targetFormMetadata = downloadResult.getLeft()
          .withUrls(sourceFormMetadata.getManifestUrl(), sourceFormMetadata.getDownloadUrl());
      SubmissionKeyGenerator subKeyGen = downloadResult.getRight();

      List<AggregateAttachment> attachments = getFormAttachments(sourceFormMetadata, rs, tracker);
      int totalAttachments = attachments.size();
      AtomicInteger attachmentNumber = new AtomicInteger(1);
      attachments.parallelStream().forEach(attachment ->
          downloadFormAttachment(attachment, targetFormMetadata.getFormMediaFile(attachment.getFilename()), rs, tracker, attachmentNumber.getAndIncrement(), totalAttachments)
      );

      List<InstanceIdBatch> instanceIdBatches = getSubmissionIds(formKey.getId(), lastCursor.orElse(Cursor.empty()), rs, tracker);

      // Extract all the instance IDs from all the batches and download each instance
      List<String> ids = instanceIdBatches.stream()
          .flatMap(batch -> batch.getInstanceIds().stream())
          .collect(toList());
      int totalSubmissions = ids.size();
      AtomicInteger submissionNumber = new AtomicInteger(1);

      if (ids.isEmpty())
        tracker.trackNoSubmissions();

      // We need to collect to be able to create a parallel stream again
      ids.parallelStream()
          .map(instanceId -> {
            return Triple.of(
                submissionNumber.getAndIncrement(),
                instanceId,
                workspace.submissionMetadata.hasBeenAlreadyPulled(formId, instanceId)
            );
          })
          .peek(triple -> {
            if (triple.get3())
              tracker.trackSubmissionAlreadyDownloaded(triple.get1(), totalSubmissions);
          })
          .filter(not(Triple::get3))
          .map(triple -> Pair.of(
              triple.get1(),
              downloadSubmission(triple.get2(), targetFormMetadata.getSubmissionFile(triple.get2()), subKeyGen, rs, tracker, triple.get1(), totalSubmissions)
          ))
          .filter(p -> p.getRight() != null)
          .forEach(pair -> {
            int currentSubmissionNumber = pair.getLeft();
            DownloadedSubmission submission = pair.getRight();
            List<AggregateAttachment> submissionAttachments = submission.getAttachments();
            AtomicInteger submissionAttachmentNumber = new AtomicInteger(1);
            int totalSubmissionAttachments = submissionAttachments.size();
            submissionAttachments.parallelStream().forEach(attachment ->
                downloadSubmissionAttachment(attachment, targetFormMetadata.getSubmissionAttachmentFile(submission.getInstanceId(), attachment.getFilename()), rs, tracker, currentSubmissionNumber, totalSubmissions, submissionAttachmentNumber.getAndIncrement(), totalSubmissionAttachments)
            );

            SubmissionMetadata submissionMetadata = SubmissionMetadata.from(
                submission.getSubmissionFile(),
                submission.getInstanceId(),
                submission.getAttachments().stream()
                    .filter(not(AggregateAttachment::isEncryptedSubmissionFile))
                    .map(AggregateAttachment::getFilename)
                    .map(Paths::get)
                    .collect(toList())
            );
            workspace.submissionMetadata.execute(insert(submissionMetadata));
          });

      tracker.trackEnd();
      Cursor newCursor = getLastCursor(instanceIdBatches).orElse(Cursor.empty());

      workspace.formMetadata.execute(upsert(targetFormMetadata.withCursor(newCursor)));

      EventBus.publish(PullEvent.Success.of(formKey, server));
    });

  }

  Pair<FormMetadata, SubmissionKeyGenerator> downloadForm(FormMetadata sourceFormMetadata, Path targetFormFile, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return null;
    }

    tracker.trackStartDownloadingForm();
    Response<String> response = workspace.http.execute(getDownloadFormRequest(sourceFormMetadata));
    if (!response.isSuccess()) {
      tracker.trackErrorDownloadingForm(response);
      return null;
    }

    createDirectories(targetFormFile.getParent());

    String formXml = response.get();
    write(targetFormFile, formXml, CREATE, TRUNCATE_EXISTING);
    tracker.trackEndDownloadingForm();
    return Pair.of(
        FormMetadata.from(targetFormFile),
        SubmissionKeyGenerator.from(formXml)
    );
  }

  private Request<String> getDownloadFormRequest(FormMetadata formMetadata) {
    return formMetadata.getDownloadUrl()
        .map(server::getDownloadFormRequest)
        .orElseGet(() -> server.getDownloadFormRequest(formMetadata.getKey().getId()));
  }

  List<AggregateAttachment> getFormAttachments(FormMetadata formMetadata, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    if (formMetadata.getManifestUrl().isEmpty())
      return emptyList();

    tracker.trackStartGettingFormManifest();
    URL manifestUrl = formMetadata.getManifestUrl().get();
    Request<List<AggregateAttachment>> request = get(manifestUrl)
        .asXmlElement()
        .withResponseMapper(PullFromAggregate::parseMediaFiles)
        .build();
    Response<List<AggregateAttachment>> response = workspace.http.execute(request);
    if (!response.isSuccess()) {
      tracker.trackErrorGettingFormManifest(response);
      return Collections.emptyList();
    }

    List<AggregateAttachment> attachments = response.get();
    List<AggregateAttachment> attachmentsToDownload = attachments.stream()
        .filter(mediaFile -> mediaFile.needsUpdate(formMetadata.getFormMediaDir()))
        .collect(toList());
    tracker.trackEndGettingFormManifest();
    tracker.trackIgnoredFormAttachments(attachmentsToDownload.size(), attachments.size());
    return attachmentsToDownload;
  }

  List<InstanceIdBatch> getSubmissionIds(String formId, Cursor lastCursor, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions IDs");
      return emptyList();
    }

    return getInstanceIdBatches(formId, runnerStatus, tracker, lastCursor);
  }

  void downloadFormAttachment(AggregateAttachment attachment, Path targetAttachmentFile, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getFilename());
      return;
    }

    createDirectories(targetAttachmentFile.getParent());

    tracker.trackStartDownloadingFormAttachment(attachmentNumber, totalAttachments);
    Response response = workspace.http.execute(get(attachment.getDownloadUrl()).downloadTo(targetAttachmentFile).build());
    if (response.isSuccess())
      tracker.trackEndDownloadingFormAttachment(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  DownloadedSubmission downloadSubmission(String instanceId, Path targetSubmissionFile, SubmissionKeyGenerator subKeyGen, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int submissionNumber, int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + submissionNumber + " of " + totalSubmissions);
      return null;
    }

    tracker.trackStartDownloadingSubmission(submissionNumber, totalSubmissions);
    String submissionKey = subKeyGen.buildKey(instanceId);
    Response<DownloadedSubmission> response = workspace.http.execute(server.getDownloadSubmissionRequest(submissionKey));
    if (!response.isSuccess()) {
      tracker.trackErrorDownloadingSubmission(submissionNumber, totalSubmissions, response);
      return null;
    }
    DownloadedSubmission submission = response.get();

    createDirectories(targetSubmissionFile.getParent());
    write(targetSubmissionFile, submission.getXml(), CREATE, TRUNCATE_EXISTING);
    tracker.trackEndDownloadingSubmission(submissionNumber, totalSubmissions);
    return submission.withSubmissionFile(targetSubmissionFile);
  }

  void downloadSubmissionAttachment(AggregateAttachment attachment, Path targetAttachmentFile, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    createDirectories(targetAttachmentFile.getParent());

    tracker.trackStartDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = workspace.http.execute(get(attachment.getDownloadUrl()).downloadTo(targetAttachmentFile).build());
    if (response.isSuccess())
      tracker.trackEndDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments, response);
  }

  private static List<AggregateAttachment> parseMediaFiles(XmlElement root) {
    return asMediaFileList(root.findElements("mediaFile"));
  }

  static List<AggregateAttachment> asMediaFileList(List<XmlElement> xmlElements) {
    return xmlElements.stream()
        .map(mediaFile -> OptionalProduct.all(
            mediaFile.findFirstElement("filename").flatMap(XmlElement::maybeValue),
            mediaFile.findFirstElement("hash").flatMap(XmlElement::maybeValue),
            mediaFile.findFirstElement("downloadUrl").flatMap(XmlElement::maybeValue)
        ).map(AggregateAttachment::of))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private List<InstanceIdBatch> getInstanceIdBatches(String formId, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, Cursor lastCursor) {
    tracker.trackStartGettingSubmissionIds();
    InstanceIdBatchGetter batchPager;
    try {
      batchPager = new InstanceIdBatchGetter(workspace, server, formId, includeIncomplete, lastCursor);
    } catch (InstanceIdBatchGetterException e) {
      tracker.trackErrorGettingInstanceIdBatches(e.aggregateResponse);
      return emptyList();
    }
    List<InstanceIdBatch> batches = new ArrayList<>();
    // The first batch is always an empty batch with the last cursor
    // to avoid losing it if there are no new submissions available
    batches.add(InstanceIdBatch.from(emptyList(), lastCursor));
    while (runnerStatus.isStillRunning() && batchPager.hasNext())
      batches.add(batchPager.next());
    tracker.trackEndGettingSubmissionIds();
    return batches;
  }

}
