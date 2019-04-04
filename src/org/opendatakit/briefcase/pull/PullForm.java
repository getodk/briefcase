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

package org.opendatakit.briefcase.pull;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullForm {
  public static final Logger log = LoggerFactory.getLogger(PullForm.class);
  private final Http http;
  private final RemoteServer server;
  private final Path briefcaseDir;
  private final boolean includeIncomplete;

  PullForm(Http http, RemoteServer server, Path briefcaseDir, boolean includeIncomplete) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.includeIncomplete = includeIncomplete;
  }

  public static Job<PullResult> pull(Http http, RemoteServer server, Path briefcaseDir, boolean includeIncomplete, FormStatus form) {
    return new PullForm(http, server, briefcaseDir, includeIncomplete).pull(form);
  }

  private Job<PullResult> pull(FormStatus form) {
    PullTracker tracker = new PullTracker(form, EventBus::publish);
    return allOf(
        supply(runnerStatus -> downloadForm(form, tracker)),
        supply(runnerStatus -> getInstanceIdBatches(form, tracker, runnerStatus)),
        run(runnerStatus -> downloadFormAttachments(form, tracker))
    ).thenApply((runnerStatus, t) -> {
      // Build the submission key generator with the blank form XML
      SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(t.get1());

      // Extract all the instance IDs from all the batches and download each instance
      t.get2().stream()
          .flatMap(batch -> batch.getInstanceIds().stream())
          .forEach(instanceId -> {
            if (runnerStatus.isStillRunning())
              downloadSubmissionAndMedia(form, tracker, instanceId, subKeyGen);
          });

      // Return the pull result with the last cursor
      return PullResult.of(form, getLastCursor(t.get2()));
    });
  }

  private static String getLastCursor(List<InstanceIdBatch> batches) {
    return batches.stream()
        .map(InstanceIdBatch::getCursor)
        .reduce(maxBy(Cursor::compareTo))
        .orElseThrow(BriefcaseException::new)
        .get();
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

  String downloadForm(FormStatus form, PullTracker tracker) {
    String formXml = http.execute(server.getDownloadFormRequest(form.getFormId())).get();
    writeForm(form, formXml);
    tracker.trackFormDownloaded();
    return formXml;
  }

  private List<InstanceIdBatch> getInstanceIdBatches(FormStatus form, PullTracker tracker, RunnerStatus runnerStatus) {
    List<InstanceIdBatch> batches = new ArrayList<>();
    InstanceIdBatchGetter batchPager = new InstanceIdBatchGetter(server, http, form.getFormId(), includeIncomplete);
    while (runnerStatus.isStillRunning() && batchPager.hasNext())
      batches.add(batchPager.next());
    tracker.trackBatches(batches);
    return batches;
  }

  void downloadFormAttachments(FormStatus form, PullTracker tracker) {
    form.getManifestUrl()
        .filter(RequestBuilder::isUri)
        .ifPresent(manifestUrl -> {
          Path mediaDir = form.getFormMediaDir(briefcaseDir);
          if (!exists(mediaDir))
            createDirectories(mediaDir);
          downloadMediaFiles(
              http.execute(RequestBuilder.get(manifestUrl).asXmlElement().withMapper(PullForm::parseMediaFiles).build()).get(),
              mediaDir,
              tracker
          );
        });
  }

  void downloadSubmissionAndMedia(FormStatus form, PullTracker tracker, String instanceId, SubmissionKeyGenerator subKeyGen) {
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

  private void downloadSubmissionAttachments(FormStatus form, DownloadedSubmission submission, PullTracker tracker) {
    Path mediaDir = form.getSubmissionDir(briefcaseDir, submission.getInstanceId());
    if (!exists(mediaDir))
      createDirectories(mediaDir);
    List<MediaFile> mediaFiles = submission.getAttachments();
    downloadMediaFiles(mediaFiles, mediaDir, tracker);
  }

  private void downloadMediaFiles(List<MediaFile> mediaFiles, Path mediaDir, PullTracker tracker) {
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
