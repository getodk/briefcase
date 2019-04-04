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

package org.opendatakit.briefcase.pull;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.pull.InstanceIdBatchGetter.getInstanceIdBatches;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullForm {
  private static final Logger log = LoggerFactory.getLogger(PullForm.class);

  public static String pull(FormStatus form, boolean includeIncomplete, RemoteServer server, Path briefcaseDir, Http http) {
    // Download the blank form
    String formXml = downloadForm(form, server, http);
    writeForm(form, briefcaseDir, formXml);

    // Download attachments of the blank form
    downloadFormAttachments(form, briefcaseDir, http);

    // Get all the submission batches
    List<InstanceIdBatch> batches = getInstanceIdBatches(server, http, form.getFormId(), includeIncomplete);

    // For all submissions in all batches...
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(formXml);
    batches.stream().flatMap(batch -> batch.getInstanceIds().stream()).forEach(instanceId -> {
      // Download the submission
      DownloadedSubmission submission = downloadSubmission(form, server, briefcaseDir, http, subKeyGen, instanceId);
      writeSubmission(form, submission, briefcaseDir);

      // Download attachments of the submission
      downloadSubmissionAttachments(form, submission, briefcaseDir, http);
    });

    // Return the last cursor received
    return batches.stream()
        .map(InstanceIdBatch::getCursor)
        .reduce(maxBy(Cursor::compareTo))
        .orElseThrow(BriefcaseException::new)
        .get();
  }

  private static String downloadForm(FormStatus form, RemoteServer server, Http http) {
    return http.execute(server.getDownloadFormRequest(form.getFormId())).get();
  }

  private static DownloadedSubmission downloadSubmission(FormStatus form, RemoteServer server, Path briefcaseDir, Http http, SubmissionKeyGenerator subKeyGen, String instanceId) {
    Path instanceDir = form.getSubmissionDir(briefcaseDir, instanceId);
    if (!exists(instanceDir))
      createDirectories(instanceDir);
    String submissionKey = subKeyGen.buildKey(instanceId);
    log.info("Got xml for instance ID {} of form {}", instanceId, form.getFormId());
    return http.execute(server.getDownloadSubmissionRequest(submissionKey)).orElseThrow(BriefcaseException::new);
  }

  private static void downloadSubmissionAttachments(FormStatus form, DownloadedSubmission submission, Path briefcaseDir, Http http) {
    Path mediaDir = form.getSubmissionDir(briefcaseDir, submission.getInstanceId());
    if (!exists(mediaDir))
      createDirectories(mediaDir);
    downloadMediaFiles(submission.getAttachments(), mediaDir, http);
  }

  private static void downloadFormAttachments(FormStatus form, Path briefcaseDir, Http http) {
    form.getManifestUrl().ifPresent(manifestUrl -> {
      Path mediaDir = form.getFormMediaDir(briefcaseDir);
      if (!exists(mediaDir))
        createDirectories(mediaDir);
      List<MediaFile> mediaFiles = http.execute(get(manifestUrl).asXmlElement().withMapper(PullForm::parseMediaFiles).build()).get();
      downloadMediaFiles(mediaFiles, mediaDir, http);
    });
  }

  private static void downloadMediaFiles(List<MediaFile> mediaFiles, Path mediaDir, Http http) {
    mediaFiles.stream()
        .filter(mediaFile -> mediaFile.needsUpdate(mediaDir))
        .forEach(mediaFile -> {
          Path target = mediaFile.getTargetPath(mediaDir);
          http.execute(get(mediaFile.getDownloadUrl()).downloadTo(target).build());
          log.info("Downloaded mediaFile at {}", target);
        });
  }

  private static void writeForm(FormStatus form, Path briefcaseDir, String formXml) {
    Path formDir = form.getFormDir(briefcaseDir);
    if (!exists(formDir))
      createDirectories(formDir);
    Path formFile = formDir.resolve(stripIllegalChars(form.getFormName()) + ".xml");
    write(formFile, formXml, CREATE, TRUNCATE_EXISTING);
    log.info("Wrote blank form at {}", formFile);
  }

  private static void writeSubmission(FormStatus form, DownloadedSubmission submission, Path briefcaseDir) {
    Path submissionFile = form.getSubmissionDir(briefcaseDir, submission.getInstanceId()).resolve("submission.xml");
    write(submissionFile, submission.getContent(), CREATE, TRUNCATE_EXISTING);
    log.info("Wrote submission at {}", submissionFile);
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
}
