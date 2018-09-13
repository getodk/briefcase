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
 * <p>
 * All its members are lazily evaluated to avoid unnecessary parsing.
 */
class SubmissionMetaData {
  private final XmlElement root;
  // All these members are not final because they're lazily evaluated
  private String formId;
  // TODO Make explicit that these members are lazily initialized
  private Optional<String> instanceId;
  private Optional<String> version;
  private Optional<OffsetDateTime> submissionDate;
  private Optional<String> encryptedXmlFile;
  private Optional<String> base64EncryptedKey;
  private Optional<String> encryptedSignature;
  private List<String> mediaNames;

  /**
   * Main constructor for {@link SubmissionMetaData} class. It takes
   * an {@link XmlElement} to act as the root element which will be
   * queries for the different values this class can offer.
   */
  SubmissionMetaData(XmlElement root) {
    this.root = root;
  }

  /**
   * Returns the submission date, located at the root node's "submissionDate" attribute.
   * <p>
   * The value gets mapped to an {@link OffsetDateTime}
   */
  Optional<OffsetDateTime> getSubmissionDate() {
    if (submissionDate == null)
      submissionDate = root.getAttributeValue("submissionDate")
          .map(SubmissionMetaData::regularizeDateTime)
          .map(OffsetDateTime::parse);
    return submissionDate;
  }

  /** Fixes ISO8601-ish strings not in this form: 2018-05-13T17:32:57±00:00 */
  static String regularizeDateTime(String iso8601DateTime) {
    // 2018-04-26T08:58:20.525Z
    if (iso8601DateTime.endsWith("Z")) // Replace the Z with a zero offset
      return iso8601DateTime.substring(0, iso8601DateTime.length() - 1) + "+00:00";

    // 2018-05-13T17:32:57+00  Add the minutes if not present
    return iso8601DateTime + (iso8601DateTime.length() - iso8601DateTime.lastIndexOf(":") == 3 ? "" : ":00");
  }

  /**
   * Returns this submission's instance ID, which is taken from the &lt;instanceID&gt;
   * element's value, or from the root node's "instanceID" attribute.
   */
  Optional<String> getInstanceId() {
    if (instanceId == null)
      instanceId = Optionals.race(
          root.findElement("instanceID").flatMap(XmlElement::maybeValue),
          root.getAttributeValue("instanceID")
      );
    return instanceId;
  }

  /**
   * Returns this submission's form ID, which is taken from the root node's "id" or
   * "xmlns" attribute.
   *
   * @throws ParsingException if neither attribute is found or they're empty
   */
  String getFormId() {
    if (formId == null)
      formId = Optionals.race(
          root.getAttributeValue("id"),
          root.getAttributeValue("xmlns")
      ).orElseThrow(() -> new ParsingException("Unable to extract form id"));
    return formId;
  }

  /**
   * Return this submission's version, which is taken from the root node's
   * "version" attribute.
   */
  Optional<String> getVersion() {
    if (version == null)
      version = root.getAttributeValue("version");
    return version;
  }

  /**
   * Return the base64 encoded encryption key from the &lt;base64EncryptedKey&gt;
   * element's value.
   */
  Optional<String> getBase64EncryptedKey() {
    if (base64EncryptedKey == null)
      base64EncryptedKey = root.findElement("base64EncryptedKey").flatMap(XmlElement::maybeValue);
    return base64EncryptedKey;
  }

  /**
   * Returns the list of media attachment file names, which are the values of
   * all the &lt;file&gt; children in the &lt;media&gt element
   */
  List<String> getMediaNames() {
    if (mediaNames == null)
      mediaNames = root.findElements("media", "file").stream()
          .map(XmlElement::maybeValue)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(toList());
    return mediaNames;
  }

  /**
   * Return the file name of the encrypted submission file, taken from the
   * &lt;encryptedXmlFile&gt; element's value.
   */
  Optional<String> getEncryptedXmlFile() {
    if (encryptedXmlFile == null)
      encryptedXmlFile = root.findElement("encryptedXmlFile").flatMap(XmlElement::maybeValue);
    return encryptedXmlFile;
  }

  /**
   * Return the cryptographic signature of this submissions, taken from the
   * &lt;base64EncryptedElementSignature&gt; element's value.
   */
  Optional<String> getEncryptedSignature() {
    if (encryptedSignature == null)
      encryptedSignature = root.findElement("base64EncryptedElementSignature").flatMap(XmlElement::maybeValue);
    return encryptedSignature;
  }
}
