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

package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate.asMediaFileList;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.submission.SubmissionLazyMetadata;

/**
 * Stores a form submission's contents and a list to its attachments.
 */
public class DownloadedSubmission {
  private final String xml;
  private final String instanceId;
  private final List<AggregateAttachment> attachments;
  private final Optional<Path> submissionFile;

  DownloadedSubmission(String xml, String instanceId, List<AggregateAttachment> attachments, Optional<Path> submissionFile) {
    this.xml = xml;
    this.instanceId = instanceId;
    this.attachments = attachments;
    this.submissionFile = submissionFile;
  }

  /**
   * Returns a new DownloadedSubmission instance by extracting the primary
   * instance and a list of attachments from a submission download document,
   * as described in the <a href="https://docs.opendatakit.org/briefcase-api/#response-document">Briefcase Aggregate API docs</a>.
   * <p>
   * The instance is then serialized to produce the XML document that
   * ultimately will be saved to the local filesystem.
   */
  public static DownloadedSubmission from(XmlElement submission) {
    XmlElement instance = submission.findElement("data").orElseThrow(BriefcaseException::new).childrenOf().get(0);
    return new DownloadedSubmission(
        instance.serialize(),
        new SubmissionLazyMetadata(instance).getInstanceId().orElseThrow(BriefcaseException::new),
        asMediaFileList(submission.findElements("mediaFile")),
        Optional.empty()
    );
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

  public DownloadedSubmission withSubmissionFile(Path submissionFile) {
    return new DownloadedSubmission(xml, instanceId, attachments, Optional.of(submissionFile));
  }

  public Optional<Path> getSubmissionFile() {
    return submissionFile;
  }
}
