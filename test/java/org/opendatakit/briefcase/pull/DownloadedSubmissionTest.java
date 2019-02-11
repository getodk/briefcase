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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.opendatakit.briefcase.export.XmlElement;

public class DownloadedSubmissionTest {

  private static String buildMediaFileXml() {
    return "" +
        "  <mediaFile>\n" +
        "    <filename>some-filename.ext</filename>\n" +
        "    <hash>some-hash</hash>\n" +
        "    <downloadUrl>http://foo.bar</downloadUrl>\n" +
        "  </mediaFile>" +
        "";
  }

  @Test
  public void parses_the_download_submission_response_from_a_remote_server() {
    String expectedInstanceId = "uuid:" + UUID.randomUUID().toString();
    DownloadedSubmission ds = DownloadedSubmission.from(XmlElement.from(buildXml(expectedInstanceId, 3)));
    assertThat(ds.getInstanceId(), is(expectedInstanceId));
    assertThat(ds.getAttachments(), hasSize(3));
  }

  private String buildXml(String instanceId, int mediaFiles) {
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
        IntStream.range(0, mediaFiles).mapToObj(__ -> buildMediaFileXml()).collect(Collectors.joining("\n")) +
        "</submission>" +
        "";
  }
}
