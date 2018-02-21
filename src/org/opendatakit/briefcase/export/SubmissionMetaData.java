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
package org.opendatakit.briefcase.export;

import static java.util.stream.Collectors.toList;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.reused.Optionals;

/**
 * This class holds a form's metadata. Instances of this class are
 * generated while parsing submissions and they are used to read
 * each value only once.
 */
class SubmissionMetaData {
  final String formId;
  final Optional<String> instanceId;
  final Optional<String> version;
  final Optional<OffsetDateTime> submissionDate;
  final Optional<String> encriptedSubmissionFileName;
  final Optional<String> base64EncryptedKey;
  final Optional<String> encryptedSignature;
  final List<String> mediaNames;

  private SubmissionMetaData(String formId, Optional<String> instanceId, Optional<String> version, Optional<OffsetDateTime> submissionDate, Optional<String> encriptedSubmissionFileName, Optional<String> base64EncryptedKey, Optional<String> encryptedSignature, List<String> mediaNames) {
    this.formId = formId;
    this.instanceId = instanceId;
    this.version = version;
    this.submissionDate = submissionDate;
    this.encriptedSubmissionFileName = encriptedSubmissionFileName;
    this.base64EncryptedKey = base64EncryptedKey;
    this.encryptedSignature = encryptedSignature;
    this.mediaNames = mediaNames;
  }

  /**
   * Factory that produces new instances of {@link SubmissionMetaData} reading
   * a root {@link XmlElement}.
   *
   * @param root the {@link XmlElement} root element to be read
   * @return a new {@link SubmissionMetaData} instance
   */
  static SubmissionMetaData from(XmlElement root) {
    return new SubmissionMetaData(
        Optionals.race(
            root.getAttributeValue("id"),
            root.getAttributeValue("xmlns")
        ).orElseThrow(() -> new ParsingException("Unable to extract form id")),
        Optionals.race(
            root.findElement("instanceID").flatMap(XmlElement::maybeValue),
            root.getAttributeValue("instanceID")
        ),
        root.getAttributeValue("version"),
        root.getAttributeValue("submissionDate")
            .map(SubmissionMetaData::fixISO8601DateTimeString)
            .map(OffsetDateTime::parse),
        root.findElement("encryptedXmlFile").flatMap(XmlElement::maybeValue),
        root.findElement("base64EncryptedKey").flatMap(XmlElement::maybeValue),
        root.findElement("base64EncryptedElementSignature").flatMap(XmlElement::maybeValue),
        root.findElements("media", "file").stream()
            .map(XmlElement::maybeValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList())
    );
  }

  private static String fixISO8601DateTimeString(String iso8601DateTime) {
    return iso8601DateTime.length() - iso8601DateTime.lastIndexOf("+") == 3 ? iso8601DateTime + ":00" : iso8601DateTime;
  }

  public boolean isEncrypted() {
    return base64EncryptedKey.isPresent();
  }
}
