/*
 * Copyright (C) 2018 Nafundi
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
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.job.Job.allOf;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.job.Job.supply;
import static org.opendatakit.briefcase.util.DatabaseUtils.withDb;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromAggregate {
  public static final Logger log = LoggerFactory.getLogger(PullFromAggregate.class);
  private final Http http;
  private final AggregateServer server;
  private final Path briefcaseDir;
  private final boolean includeIncomplete;

  public PullFromAggregate(Http http, AggregateServer server, Path briefcaseDir, boolean includeIncomplete) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.includeIncomplete = includeIncomplete;
  }

  public static Job<PullFromAggregateResult> pull(Http http, AggregateServer server, Path briefcaseDir, boolean includeIncomplete, Consumer<FormStatusEvent> onEventCallback, FormStatus form, Optional<Cursor> lastCursor) {
    return new PullFromAggregate(http, server, briefcaseDir, includeIncomplete).pull(form, lastCursor, onEventCallback);
  }

  public Job<PullFromAggregateResult> pull(FormStatus form, Optional<Cursor> lastCursor, Consumer<FormStatusEvent> onEventCallback) {
    PullFromAggregateTracker tracker = new PullFromAggregateTracker(form, onEventCallback);
    return withDb(form.getFormDir(briefcaseDir), db -> allOf(
        supply(runnerStatus -> downloadForm(form, tracker)),
        supply(runnerStatus -> getInstanceIdBatches(form, lastCursor.orElse(Cursor.empty()), tracker, runnerStatus)),
        run(runnerStatus -> downloadFormAttachments(form, tracker))
    ).thenApply((runnerStatus, t) -> {
      // Build the submission key generator with the blank form XML
      SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(t.get1());

      // Extract all the instance IDs from all the batches and download each instance
      t.get2().stream()
          .flatMap(batch -> batch.getInstanceIds().stream())
          .filter(instanceId -> db.hasRecordedInstance(instanceId) == null)
          .forEach(instanceId -> {
            if (runnerStatus.isStillRunning()) {
              downloadSubmissionAndMedia(form, tracker, instanceId, subKeyGen);
              db.putRecordedInstanceDirectory(instanceId, form.getSubmissionDir(briefcaseDir, instanceId).toFile());
            }
          });

      // Return the pull result with the last cursor
      return PullFromAggregateResult.of(form, getLastCursor(t.get2()));
    }));
  }

  private static Cursor getLastCursor(List<InstanceIdBatch> batches) {
    return batches.stream()
        .map(InstanceIdBatch::getCursor)
        .reduce(maxBy(Cursor::compareTo))
        .orElseThrow(BriefcaseException::new);
  }

  private static List<MediaFile> parseMediaFiles(XmlElement root) {
    return asMediaFileList(root.findElement("manifest")
        .map(manifest -> manifest.findElements("mediaFile"))
        .orElse(emptyList()));
  }

  static List<MediaFile> asMediaFileList(List<XmlElement> xmlElements) {
    return xmlElements.stream()
        .map(mediaFile -> OptionalProduct.all(
            mediaFile.findElement("filename").flatMap(XmlElement::maybeValue),
            mediaFile.findElement("hash").flatMap(XmlElement::maybeValue),
            mediaFile.findElement("downloadUrl").flatMap(XmlElement::maybeValue)
        ).map(MediaFile::of))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  String downloadForm(FormStatus form, PullFromAggregateTracker tracker) {
    String formXml = http.execute(server.getDownloadFormRequest(form.getFormId())).get();
    writeForm(form, formXml);
    tracker.trackFormDownloaded();
    return formXml;
  }

  private List<InstanceIdBatch> getInstanceIdBatches(FormStatus form, Cursor lastCursor, PullFromAggregateTracker tracker, RunnerStatus runnerStatus) {
    List<InstanceIdBatch> batches = new ArrayList<>();
    InstanceIdBatchGetter batchPager = new InstanceIdBatchGetter(server, http, form.getFormId(), includeIncomplete, lastCursor);
    while (runnerStatus.isStillRunning() && batchPager.hasNext())
      batches.add(batchPager.next());
    tracker.trackBatches(batches);
    return batches;
  }

  void downloadFormAttachments(FormStatus form, PullFromAggregateTracker tracker) {
    form.getManifestUrl()
        .filter(RequestBuilder::isUri)
        .ifPresent(manifestUrl -> {
          Path mediaDir = form.getFormMediaDir(briefcaseDir);
          if (!exists(mediaDir))
            createDirectories(mediaDir);
          downloadMediaFiles(
              http.execute(RequestBuilder.get(manifestUrl).asXmlElement().withResponseMapper(PullFromAggregate::parseMediaFiles).build()).get(),
              mediaDir,
              tracker
          );
        });
  }

  void downloadSubmissionAndMedia(FormStatus form, PullFromAggregateTracker tracker, String instanceId, SubmissionKeyGenerator subKeyGen) {
    DownloadedSubmission submission = downloadSubmission(form, subKeyGen, instanceId);
    writeSubmission(form, submission);
    downloadSubmissionAttachments(form, submission, tracker);
    tracker.trackSubmission();
  }

  private DownloadedSubmission downloadSubmission(FormStatus form, SubmissionKeyGenerator subKeyGen, String instanceId) {
    Path instanceDir = form.getSubmissionDir(briefcaseDir, instanceId);
    if (!Files.exists(instanceDir))
      createDirectories(instanceDir);
    String submissionKey = subKeyGen.buildKey(instanceId);
    return http.execute(server.getDownloadSubmissionRequest(submissionKey)).orElseThrow(BriefcaseException::new);
  }

  private void downloadSubmissionAttachments(FormStatus form, DownloadedSubmission submission, PullFromAggregateTracker tracker) {
    Path mediaDir = form.getSubmissionDir(briefcaseDir, submission.getInstanceId());
    if (!exists(mediaDir))
      createDirectories(mediaDir);
    List<MediaFile> mediaFiles = submission.getAttachments();
    downloadMediaFiles(mediaFiles, mediaDir, tracker);
  }

  private void downloadMediaFiles(List<MediaFile> mediaFiles, Path mediaDir, PullFromAggregateTracker tracker) {
    List<MediaFile> mediaFilesToDownload = mediaFiles.stream().filter(mediaFile -> mediaFile.needsUpdate(mediaDir)).collect(Collectors.toList());
    mediaFilesToDownload.forEach(mediaFile -> {
      Path target = mediaFile.getTargetPath(mediaDir);
      http.execute(get(mediaFile.getDownloadUrl()).downloadTo(target).build());
    });
    tracker.trackMediaFiles(mediaFiles, mediaFilesToDownload);
  }

  private void writeForm(FormStatus form, String blankFormXml) {
    Path formDir = form.getFormDir(briefcaseDir);
    if (!Files.exists(formDir))
      createDirectories(formDir);
    Path formFile = formDir.resolve(StringUtils.stripIllegalChars(form.getFormName()) + ".xml");
    write(formFile, blankFormXml, CREATE, TRUNCATE_EXISTING);
  }

  private void writeSubmission(FormStatus form, DownloadedSubmission submission) {
    Path submissionFile = form.getSubmissionDir(briefcaseDir, submission.getInstanceId()).resolve("submission.xml");
    write(submissionFile, submission.getXml(), CREATE, TRUNCATE_EXISTING);
  }
}
