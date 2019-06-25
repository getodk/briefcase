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
package org.opendatakit.briefcase.reused.transfer;

import static java.lang.Math.min;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.pull.aggregate.AggregateAttachment;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.Pair;

public class TransferTestHelpers {
  public static String buildSubmissionXml(String instanceId) {
    return "" +
        "<some-form id=\"some-form\" instanceID=\"" + instanceId + "\" submissionDate=\"2018-07-19T10:36:50.779Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-07-19T10:36:50.779Z\">\n" +
        "  <orx:meta>\n" +
        "    <orx:instanceID>" + instanceId + "</orx:instanceID>\n" +
        "  </orx:meta>\n" +
        "  <some-field>some value</some-field>\n" +
        "</some-form>" +
        "";
  }

  public static String buildAggregateSubmissionDownloadXml(String instanceId, int mediaFiles) {
    return "" +
        "<submission xmlns=\"http://opendatakit.org/submissions\" xmlns:orx=\"http://openrosa.org/xforms\">\n" +
        "  <data>\n" +
        "    <some-form id=\"some-form\" instanceID=\"" + instanceId + "\" submissionDate=\"2018-07-19T10:36:50.779Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-07-19T10:36:50.779Z\">\n" +
        "      <orx:meta>\n" +
        "        <orx:instanceID>" + instanceId + "</orx:instanceID>\n" +
        "      </orx:meta>\n" +
        "      <some-field>some value</some-field>\n" +
        "    </some-form>" +
        "  </data>\n" +
        IntStream.range(0, mediaFiles).mapToObj(TransferTestHelpers::buildMediaFileXml).collect(Collectors.joining("\n")) +
        "</submission>" +
        "";
  }

  public static String buildAggregateSubmissionDownloadXml(String instanceId, List<AggregateAttachment> attachments) {
    return "" +
        "<submission xmlns=\"http://opendatakit.org/submissions\" xmlns:orx=\"http://openrosa.org/xforms\">\n" +
        "  <data>\n" +
        "    <some-form id=\"some-form\" instanceID=\"" + instanceId + "\" submissionDate=\"2018-07-19T10:36:50.779Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-07-19T10:36:50.779Z\">\n" +
        "      <orx:meta>\n" +
        "        <orx:instanceID>" + instanceId + "</orx:instanceID>\n" +
        "      </orx:meta>\n" +
        "      <some-field>some value</some-field>\n" +
        "    </some-form>" +
        "  </data>\n" +
        attachments.stream().map(TransferTestHelpers::buildMediaFileXml).collect(Collectors.joining("\n")) +
        "</submission>" +
        "";
  }

  private static String buildMediaFileXml(int i) {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>some-filename-" + i + ".txt</filename>\n" +
        "    <hash>some-hash</hash>\n" +
        "    <downloadUrl>http://foo.bar</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  public static String buildMediaFileXml(AggregateAttachment attachment) {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>" + attachment.getFilename() + "</filename>\n" +
        "    <hash>" + attachment.getHash() + "</hash>\n" +
        "    <downloadUrl>" + attachment.getDownloadUrl() + "</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  public static String buildMediaFileXml(String name) {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>" + name + "</filename>\n" +
        "    <hash>some hash</hash>\n" +
        "    <downloadUrl>some download url</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  public static List<AggregateAttachment> buildMediaFiles(String baseUrl, int mediaFiles) {
    return IntStream.range(0, mediaFiles).boxed()
        .map(i -> AggregateAttachment.of("some-file-" + i + ".txt", "some-hash", baseUrl + "/file/" + i))
        .collect(toList());
  }

  public static String buildManifestXml(List<AggregateAttachment> attachments) {
    return "<manifest>\n" +
        attachments.stream().map(TransferTestHelpers::buildMediaFileXml).collect(joining("\n")) + "\n" +
        "</manifest>";
  }

  public static FormStatus buildFormStatus(String formName, String manifestUrl) {
    return new FormStatus(new RemoteFormDefinition(
        formName,
        formName,
        null,
        manifestUrl
    ));
  }

  public static String buildBlankFormXml(String formId, String version, final String instanceName) {
    return "" +
        "<html>" +
        "<head>" +
        "<model>" +
        "<instance>" +
        "<" + instanceName + " id=\"" + formId + "\" version=\"" + version + "\">" +
        "</" + instanceName + ">" +
        "</instance>" +
        "</model>" +
        "</head>" +
        "<body>" +
        "</body>" +
        "</html>" +
        "";
  }

  public static String buildEncryptedBlankFormXml(String formId, String version, final String instanceName) {
    return "" +
        "<html>" +
        "<head>" +
        "<model>" +
        "<instance>" +
        "<" + instanceName + " id=\"" + formId + "\" version=\"" + version + "\">" +
        "</" + instanceName + ">" +
        "</instance>" +
        "<submission base64RsaPublicKey=\"some key\">" +
        "</submission>" +
        "</model>" +
        "</head>" +
        "<body>" +
        "</body>" +
        "</html>" +
        "";
  }

  private static String escape(String xml) {
    return xml
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\n", "");

  }

  public static List<Pair<String, Cursor>> generatePages(int totalIds, int idsPerPage) {
    AtomicInteger idSeq = new AtomicInteger(0);
    OffsetDateTime startingDateTime = OffsetDateTime.parse("2010-01-01T00:00:00.000Z");
    Cursor lastCursor = Cursor.empty();
    List<Pair<String, Cursor>> pages = new ArrayList<>();
    for (int page : IntStream.range(0, (totalIds / idsPerPage) + 1).boxed().collect(Collectors.toList())) {
      int from = page * idsPerPage;
      int to = min(totalIds, (page + 1) * idsPerPage);

      List<String> ids = IntStream.range(from, to).mapToObj(i -> buildSequentialUid(idSeq.getAndIncrement())).collect(Collectors.toList());
      Cursor cursor = Cursor.of(startingDateTime.plusDays(to - 1), buildSequentialUid(idSeq.get()));

      pages.add(Pair.of("" +
          "<idChunk xmlns=\"http://opendatakit.org/submissions\">" +
          "<idList>" + ids.stream().map(id -> "<id>" + id + "</id>").collect(joining("")) + "</idList>" +
          "<resumptionCursor>" + escape(cursor.get()) + "</resumptionCursor>" +
          "</idChunk>" +
          "", cursor));

      lastCursor = cursor;
    }
    pages.add(Pair.of("" +
        "<idChunk xmlns=\"http://opendatakit.org/submissions\">" +
        "<idList></idList>" +
        "<resumptionCursor>" + escape(lastCursor.get()) + "</resumptionCursor>" +
        "</idChunk>" +
        "", lastCursor));

    return pages;
  }

  public static String buildSequentialUid(int seq) {
    return "some sequential uuid " + seq;
  }

  public static String listOfFormsResponseFromAggregate(FormStatus... forms) {
    return "" +
        "<xforms>" +
        Stream.of(forms)
            .map(formDef -> String.format("" +
                    "\t<xform>" +
                    "\t\t<name>%s</name>" +
                    "\t\t<formID>%s</formID>" +
                    "\t\t<version>%s</version>" +
                    "\t\t<downloadUrl>%s</downloadUrl>" +
                    "\t\t<manifestUrl>%s</manifestUrl>" +
                    "\t</xform>",
                formDef.getFormName(),
                formDef.getFormId(),
                formDef.getFormDefinition().getVersionString(),
                "http://foo.bar",
                "http://foo.bar"
            ))
            .collect(joining("\n")) +
        "</xforms>" +
        "";
  }

  public static String listOfFormsResponseFromCentral(FormStatus... forms) {
    return "[\n" +
        Stream.of(forms)
            .map(form -> String.format("" +
                    "\t{\n" +
                    "\t  \"xmlFormId\": \"%s\",\n" +
                    "\t  \"name\": \"%s\",\n" +
                    "\t  \"version\": %s\n" +
                    "\t}",
                form.getFormId(),
                form.getFormName(),
                form.getFormDefinition().getVersionString() == null ? "null" : "\"" + form.getFormDefinition().getVersionString() + "\""
            ))
            .collect(joining(",\n"))
        + "\n]";
  }

  public static Path installForm(FormStatus form, Path source, Path briefcaseDir) throws IOException {
    createDirectories(form.getFormDir(briefcaseDir));
    return copy(source, form.getFormFile(briefcaseDir));
  }

  public static Path installFormAttachment(FormStatus form, Path source, Path briefcaseDir) throws IOException {
    createDirectories(form.getFormMediaDir(briefcaseDir));
    return copy(source, form.getFormMediaFile(briefcaseDir, source.getFileName().toString()));
  }

  public static Path installSubmission(FormStatus form, Path source, Path briefcaseDir) throws IOException {
    String instanceId = new SubmissionMetaData(XmlElement.from(new String(readAllBytes(source))))
        .getInstanceId()
        .orElseThrow(RuntimeException::new);
    Path submissionDir = form.getSubmissionDir(briefcaseDir, instanceId);
    createDirectories(submissionDir);
    return copy(source, form.getSubmissionFile(briefcaseDir, instanceId));
  }

  public static Path installSubmissionAttachment(FormStatus form, Path source, Path briefcaseDir, String instanceId) throws IOException {
    createDirectories(form.getSubmissionMediaDir(briefcaseDir, instanceId));
    return copy(source, form.getSubmissionMediaFile(briefcaseDir, instanceId, source.getFileName().toString()));
  }

  public static Path getResourcePath(String filename) {
    return Optional.ofNullable(TransferTestHelpers.class.getClassLoader().getResource(filename.startsWith("/") ? filename.substring(1) : filename))
        .map(url -> Paths.get(toURI(url)))
        .orElseThrow(RuntimeException::new);
  }

}
