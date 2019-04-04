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

import static org.opendatakit.briefcase.pull.PullForm.asMediaFileList;

import java.util.List;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class DownloadedSubmission {
  private final String content;
  private final String instanceId;
  private final List<MediaFile> attachments;

  private DownloadedSubmission(String content, String instanceId, List<MediaFile> attachments) {
    this.content = content;
    this.instanceId = instanceId;
    this.attachments = attachments;
  }

  public static DownloadedSubmission from(XmlElement submission) {
    XmlElement instance = submission.findElement("data").orElseThrow(BriefcaseException::new).childrenOf().get(0);
    return new DownloadedSubmission(
        instance.serialize(),
        new SubmissionMetaData(instance).getInstanceId().orElseThrow(BriefcaseException::new),
        asMediaFileList(submission.findElements("mediaFile"))
    );
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getContent() {
    return content;
  }

  List<MediaFile> getAttachments() {
    return attachments;
  }
}
