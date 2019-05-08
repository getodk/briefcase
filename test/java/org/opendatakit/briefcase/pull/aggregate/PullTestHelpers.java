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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.reused.Pair;

class PullTestHelpers {
  static String buildSubmissionXml(String instanceId, int mediaFiles) {
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
        IntStream.range(0, mediaFiles).mapToObj(i -> buildMediaFileXml(i)).collect(Collectors.joining("\n")) +
        "</submission>" +
        "";
  }

  static String buildMediaFileXml(int i) {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>some-filename-" + i + ".txt</filename>\n" +
        "    <hash>some-hash</hash>\n" +
        "    <downloadUrl>http://foo.bar</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  static String buildMediaFileXml(String filename, String url) {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>" + filename + "</filename>\n" +
        "    <hash>some-hash</hash>\n" +
        "    <downloadUrl>" + url + "</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  static Pair<String, List<Pair<String, String>>> buildManifest(String baseUrl, int mediaFiles) {
    List<Pair<String, String>> pairs = new ArrayList<>();

    for (int i : IntStream.range(0, mediaFiles).boxed().collect(toList()))
      pairs.add(Pair.of("some-file-" + i + ".txt", baseUrl + "/file/" + i));

    String mediaFilesXml = pairs.stream()
        .map(i -> buildMediaFileXml(i.getLeft(), i.getRight()))
        .collect(joining("\n"));
    return Pair.of(
        "<blankForm>" +
            "<manifest>\n" +
            mediaFilesXml + "\n" +
            "</manifest>" +
            "</blankForm>",
        pairs
    );
  }

  static FormStatus buildFormStatus(String manifestUrl) {
    return new FormStatus(new RemoteFormDefinition(
        "some form",
        "some-form",
        null,
        "",
        manifestUrl
    ));
  }

  static String buildBlankFormXml(String formId, String version, final String instanceName) {
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

  static String buildEncryptedBlankFormXml(String formId, String version, final String instanceName) {
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
}
