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

package org.opendatakit.briefcase.pull.aggregate;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateAsPulled;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.util.DatabaseUtils.withDb;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.Triple;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;

public class PullFromAggregate {
  private final Http http;
  private final AggregateServer server;
  private final Path briefcaseDir;
  private final boolean includeIncomplete;
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadataPort formMetadataPort;

  public PullFromAggregate(Http http, AggregateServer server, Path briefcaseDir, boolean includeIncomplete, Consumer<FormStatusEvent> onEventCallback, FormMetadataPort formMetadataPort) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.includeIncomplete = includeIncomplete;
    this.onEventCallback = onEventCallback;
    this.formMetadataPort = formMetadataPort;
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
  public Job<Void> pull(FormStatus form, Optional<Cursor> lastCursor) {
    FormKey key = FormKey.from(form);

    PullFromAggregateTracker tracker = new PullFromAggregateTracker(form, onEventCallback);

    // Download the form and attachments, and get the submissions list
    return run(rs -> tracker.trackStart())
        .thenSupply(rs -> downloadForm(form, rs, tracker))
        .thenAccept((rs, formXml) -> {
          if (formXml == null)
            return;

          List<AggregateAttachment> attachments = getFormAttachments(form, rs, tracker);
          int totalAttachments = attachments.size();
          AtomicInteger attachmentNumber = new AtomicInteger(1);
          attachments.parallelStream().forEach(attachment ->
              downloadFormAttachment(form, attachment, rs, tracker, attachmentNumber.getAndIncrement(), totalAttachments)
          );

          List<InstanceIdBatch> instanceIdBatches = getSubmissionIds(form, lastCursor.orElse(Cursor.empty()), rs, tracker);

          // Build the submission key generator with the form's XML contents
          SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(formXml);

          // Extract all the instance IDs from all the batches and download each instance
          List<String> ids = instanceIdBatches.stream()
              .flatMap(batch -> batch.getInstanceIds().stream())
              .collect(toList());
          int totalSubmissions = ids.size();
          AtomicInteger submissionNumber = new AtomicInteger(1);
          Set<String> submissionVersions = new HashSet<>();

          if (ids.isEmpty())
            tracker.trackNoSubmissions();

          withDb(form.getFormDir(briefcaseDir), db -> {
            // We need to collect to be able to create a parallel stream again
            ids.parallelStream()
                .map(instanceId -> Triple.of(submissionNumber.getAndIncrement(), instanceId, db.hasRecordedInstance(instanceId) == null))
                .peek(triple -> {
                  if (!triple.get3())
                    tracker.trackSubmissionAlreadyDownloaded(triple.get1(), totalSubmissions);
                })
                .filter(Triple::get3)
                .map(triple -> Pair.of(
                    triple.get1(),
                    downloadSubmission(form, triple.get2(), subKeyGen, rs, tracker, triple.get1(), totalSubmissions)
                ))
                .filter(p -> p.getRight() != null)
                .forEach(pair -> {
                  int currentSubmissionNumber = pair.getLeft();
                  DownloadedSubmission submission = pair.getRight();
                  submission.getFormVersion().ifPresent(submissionVersions::add);
                  List<AggregateAttachment> submissionAttachments = submission.getAttachments();
                  AtomicInteger submissionAttachmentNumber = new AtomicInteger(1);
                  int totalSubmissionAttachments = submissionAttachments.size();
                  submissionAttachments.parallelStream().forEach(attachment ->
                      downloadSubmissionAttachment(form, submission, attachment, rs, tracker, currentSubmissionNumber, totalSubmissions, submissionAttachmentNumber.getAndIncrement(), totalSubmissionAttachments)
                  );
                  if (!rs.isCancelled()) {
                    db.putRecordedInstanceDirectory(submission.getInstanceId(), form.getSubmissionDir(briefcaseDir, submission.getInstanceId()).toFile());
                  }
                });
          });

          tracker.trackEnd();
          Cursor newCursor = getLastCursor(instanceIdBatches).orElse(Cursor.empty());

          formMetadataPort.execute(updateAsPulled(key, newCursor, briefcaseDir, form.getFormDir(briefcaseDir), submissionVersions));

          EventBus.publish(PullEvent.Success.of(form, server));
        });

  }

  String downloadForm(FormStatus form, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form");
      return null;
    }

    tracker.trackStartDownloadingForm();
    Response<String> response = http.execute(getDownloadFormRequest(form, tracker));
    if (!response.isSuccess()) {
      tracker.trackErrorDownloadingForm(response);
      return null;
    }

    Path formFile = form.getFormFile(briefcaseDir);
    createDirectories(formFile.getParent());

    String formXml = response.get();
    write(formFile, formXml, CREATE, TRUNCATE_EXISTING);
    tracker.trackEndDownloadingForm();
    return formXml;
  }

  private Request<String> getDownloadFormRequest(FormStatus form, PullFromAggregateTracker tracker) {
    Optional<RemoteFormDefinition> maybeRemoteForm;
    if (form.getFormDefinition() instanceof RemoteFormDefinition) {
      maybeRemoteForm = Optional.of((RemoteFormDefinition) form.getFormDefinition());
    } else {
      // This is a pull before export operation. We need to get the manifest
      // to get the download URL of this blank form.
      Response<List<RemoteFormDefinition>> remoteFormsResponse = http.execute(server.getFormListRequest());
      if (remoteFormsResponse.isSuccess())
        maybeRemoteForm = remoteFormsResponse.get().stream()
            .filter(remoteForm -> remoteForm.getFormId().equals(form.getFormId()))
            .findFirst();
      else {
        tracker.trackErrorGettingFormManifest(remoteFormsResponse);
        maybeRemoteForm = Optional.empty();
      }
    }

    // In case the downloadUrl is empty, we return an Aggregate
    // compatible url and wish for the best.
    return maybeRemoteForm
        .flatMap(RemoteFormDefinition::getDownloadUrl)
        .map(server::getDownloadFormRequest)
        .orElse(server.getDownloadFormRequest(form.getFormId()));
  }

  List<AggregateAttachment> getFormAttachments(FormStatus form, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get form attachments");
      return emptyList();
    }

    if (!form.getManifestUrl().filter(RequestBuilder::isUri).isPresent())
      return emptyList();

    tracker.trackStartGettingFormManifest();
    URL manifestUrl = form.getManifestUrl().map(RequestBuilder::url).get();
    Request<List<AggregateAttachment>> request = get(manifestUrl)
        .asXmlElement()
        .withResponseMapper(PullFromAggregate::parseMediaFiles)
        .build();
    Response<List<AggregateAttachment>> response = http.execute(request);
    if (!response.isSuccess()) {
      tracker.trackErrorGettingFormManifest(response);
      return Collections.emptyList();
    }

    List<AggregateAttachment> attachments = response.get();
    List<AggregateAttachment> attachmentsToDownload = attachments.stream()
        .filter(mediaFile -> mediaFile.needsUpdate(form.getFormMediaDir(briefcaseDir)))
        .collect(toList());
    tracker.trackEndGettingFormManifest();
    tracker.trackIgnoredFormAttachments(attachmentsToDownload.size(), attachments.size());
    return attachmentsToDownload;
  }

  List<InstanceIdBatch> getSubmissionIds(FormStatus form, Cursor lastCursor, RunnerStatus runnerStatus, PullFromAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Get submissions IDs");
      return emptyList();
    }

    return getInstanceIdBatches(form, runnerStatus, tracker, lastCursor);
  }

  void downloadFormAttachment(FormStatus form, AggregateAttachment attachment, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download form attachment " + attachment.getFilename());
      return;
    }

    Path target = form.getFormMediaFile(briefcaseDir, attachment.getFilename());
    createDirectories(target.getParent());

    tracker.trackStartDownloadingFormAttachment(attachmentNumber, totalAttachments);
    Response response = http.execute(get(attachment.getDownloadUrl()).downloadTo(target).build());
    if (response.isSuccess())
      tracker.trackEndDownloadingFormAttachment(attachmentNumber, totalAttachments);
    else
      tracker.trackErrorDownloadingFormAttachment(attachmentNumber, totalAttachments, response);
  }

  DownloadedSubmission downloadSubmission(FormStatus form, String instanceId, SubmissionKeyGenerator subKeyGen, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int submissionNumber,
                                          int totalSubmissions) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download submission " + submissionNumber + " of " + totalSubmissions);
      return null;
    }

    tracker.trackStartDownloadingSubmission(submissionNumber, totalSubmissions);
    String submissionKey = subKeyGen.buildKey(instanceId);
    Response<DownloadedSubmission> response = http.execute(server.getDownloadSubmissionRequest(submissionKey));
    if (!response.isSuccess()) {
      tracker.trackErrorDownloadingSubmission(submissionNumber, totalSubmissions, response);
      return null;
    }
    DownloadedSubmission submission = response.get();

    Path submissionFile = form.getSubmissionFile(briefcaseDir, submission.getInstanceId());
    createDirectories(submissionFile.getParent());
    write(submissionFile, submission.getXml(), CREATE, TRUNCATE_EXISTING);
    tracker.trackEndDownloadingSubmission(submissionNumber, totalSubmissions);
    return submission;
  }

  void downloadSubmissionAttachment(FormStatus form, DownloadedSubmission submission, AggregateAttachment attachment, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, int submissionNumber, int totalSubmissions, int attachmentNumber, int totalAttachments) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Download attachment " + attachmentNumber + " of " + totalAttachments + " of submission " + submissionNumber + " of " + totalSubmissions);
      return;
    }

    Path target = form.getSubmissionMediaFile(briefcaseDir, submission.getInstanceId(), attachment.getFilename());
    createDirectories(target.getParent());

    tracker.trackStartDownloadingSubmissionAttachment(submissionNumber, totalSubmissions, attachmentNumber, totalAttachments);
    Response response = http.execute(get(attachment.getDownloadUrl()).downloadTo(target).build());
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
            mediaFile.findElement("filename").flatMap(XmlElement::maybeValue),
            mediaFile.findElement("hash").flatMap(XmlElement::maybeValue),
            mediaFile.findElement("downloadUrl").flatMap(XmlElement::maybeValue)
        ).map(AggregateAttachment::of))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private List<InstanceIdBatch> getInstanceIdBatches(FormStatus form, RunnerStatus runnerStatus, PullFromAggregateTracker tracker, Cursor lastCursor) {
    tracker.trackStartGettingSubmissionIds();
    InstanceIdBatchGetter batchPager;
    try {
      batchPager = new InstanceIdBatchGetter(server, http, form.getFormId(), includeIncomplete, lastCursor);
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
