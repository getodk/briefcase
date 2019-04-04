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
import static org.opendatakit.briefcase.pull.PullTestHelpers.buildSubmissionXml;

import java.util.UUID;
import org.junit.Test;
import org.opendatakit.briefcase.export.XmlElement;

public class DownloadedSubmissionTest {

  @Test
  public void parses_the_download_submission_response_from_a_remote_server() {
    String expectedInstanceId = "uuid:" + UUID.randomUUID().toString();
    DownloadedSubmission ds = DownloadedSubmission.from(XmlElement.from(buildSubmissionXml(expectedInstanceId, 3)));
    assertThat(ds.getInstanceId(), is(expectedInstanceId));
    assertThat(ds.getAttachments(), hasSize(3));
  }
}
