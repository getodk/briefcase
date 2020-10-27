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

import static org.opendatakit.briefcase.pull.aggregate.PullFromAggregate.asMediaFileList;

import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * Stores a form submission's contents and a list to its attachments.
 */
public class DownloadedSubmission {
  private final String xml;
  private final Optional<String> formVersion;
  private final String instanceId;
  private final List<AggregateAttachment> attachments;

  DownloadedSubmission(String xml, Optional<String> formVersion, String instanceId, List<AggregateAttachment> attachments) {
    this.xml = xml;
    this.formVersion = formVersion;
    this.instanceId = instanceId;
    this.attachments = attachments;
  }

  /**
   * Returns a new DownloadedSubmission instance by extracting the primary
   * instance and a list of attachments from a submission download document,
   * as described in the <a href="https://docs.getodk.org/briefcase-api/#response-document">Briefcase Aggregate API docs</a>.
   * <p>
   * The instance is then serialized to produce the XML document that
   * ultimately will be saved to the local filesystem.
   */
  public static DownloadedSubmission from(XmlElement submission) {
    XmlElement instance = submission.findElement("data").orElseThrow(BriefcaseException::new).childrenOf().get(0);
    return new DownloadedSubmission(
        instance.serialize(),
        new SubmissionMetaData(instance).getVersion(),
        new SubmissionMetaData(instance).getInstanceId().orElseThrow(BriefcaseException::new),
        asMediaFileList(submission.findElements("mediaFile"))
    );
  }

  public Optional<String> getFormVersion() {
    return formVersion;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getXml() {
    return xml;
  }

  List<AggregateAttachment> getAttachments() {
    return attachments;
  }
}
