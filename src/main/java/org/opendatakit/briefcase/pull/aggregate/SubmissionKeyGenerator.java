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

import static java.util.Collections.emptyList;

import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class generates the keys required by Aggregate to download a
 * submission, as described in the <a href="https://docs.opendatakit.org/briefcase-api/#">Briefcase Aggregate API docs</a>.
 */
// TODO v2.0 Write unit tests
public class SubmissionKeyGenerator {
  private final String formId;
  private final Optional<String> version;
  private final String submissionElementName;
  private final boolean isEncrypted;

  private SubmissionKeyGenerator(String formId, Optional<String> version, String submissionElementName, boolean isEncrypted) {
    this.formId = formId;
    this.version = version;
    this.submissionElementName = submissionElementName;
    this.isEncrypted = isEncrypted;
  }

  public static SubmissionKeyGenerator from(String xml) {
    XmlElement form = XmlElement.from(xml);
    XmlElement instance = getInstance(form);
    XmlElement submissionElement = getSubmissionElement(instance);
    return new SubmissionKeyGenerator(
        submissionElement.getAttributeValue("id").orElseThrow(BriefcaseException::new),
        submissionElement.getAttributeValue("version"),
        submissionElement.getName(),
        isEncrypted(form)
    );
  }

  private static boolean isEncrypted(XmlElement blankForm) {
    return blankForm
        .findElement("head")
        .flatMap(n -> n.findElement("model"))
        .flatMap(n -> n.findElement("submission"))
        .flatMap(n -> n.getAttributeValue("base64RsaPublicKey"))
        .isPresent();
  }

  private static XmlElement getSubmissionElement(XmlElement instance) {
    return instance
        .childrenOf()
        .get(0);
  }

  private static XmlElement getInstance(XmlElement blankForm) {
    return blankForm
        .findElement("head")
        .flatMap(n -> n.findElement("model"))
        .map(n -> n.findElements("instance"))
        .orElse(emptyList())
        .stream()
        .filter(n -> n.getAttributeValue("id").isEmpty())
        .findFirst()
        .orElseThrow(BriefcaseException::new);
  }

  String buildKey(String instanceId) {
    return String.format("%s[@version=%s and @uiVersion=null]/%s[@key=%s]",
        formId,
        version,
        isEncrypted ? "data" : submissionElementName,
        instanceId
    );
  }
}
