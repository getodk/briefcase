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
package org.opendatakit.briefcase.reused.model.submission;

import static java.util.Collections.emptyList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.getMd5Hash;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.model.submission.ValidationStatus.NOT_VALIDATED;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.model.XmlElement;

public class ParsedSubmission {
  private final SubmissionMetadata submissionMetadata;
  private final Path workingDir;
  private final XmlElement root;
  private final ValidationStatus validationStatus;
  private final Optional<CipherFactory> cipherFactory;
  private final Optional<byte[]> signature;
  private Map<String, List<XmlElement>> descendantsIndex;

  private ParsedSubmission(SubmissionMetadata submissionMetadata, Path workingDir, XmlElement root, ValidationStatus validationStatus, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    this.submissionMetadata = submissionMetadata;
    this.workingDir = workingDir;
    this.root = root;
    this.validationStatus = validationStatus;
    this.cipherFactory = cipherFactory;
    this.signature = signature;
  }

  public static ParsedSubmission plain(SubmissionMetadata submissionMetadata, XmlElement root) {
    return new ParsedSubmission(submissionMetadata, submissionMetadata.getSubmissionDir(), root, NOT_VALIDATED, Optional.empty(), Optional.empty());
  }

  static ParsedSubmission unencrypted(SubmissionMetadata submissionMetadata, XmlElement root, CipherFactory cipherFactory, byte[] signature) {
    return new ParsedSubmission(submissionMetadata, createTempDirectory("briefcase-decryption-"), root, NOT_VALIDATED, Optional.of(cipherFactory), Optional.of(signature));
  }

  SubmissionMetadata getSubmissinoMetadata() {
    return submissionMetadata;
  }

  public Path getPath() {
    return submissionMetadata.getSubmissionFile();
  }

  public Path getWorkingDir() {
    return workingDir;
  }

  public String getInstanceId() {
    return submissionMetadata.getKey().getInstanceId();
  }

  String buildSignature() {
    List<String> signatureParts = new ArrayList<>();
    signatureParts.add(submissionMetadata.getKey().getFormId());
    submissionMetadata.getKey().getFormVersion().ifPresent(signatureParts::add);
    signatureParts.add(submissionMetadata.getBase64EncryptedKey().orElseThrow(() -> new BriefcaseException("Missing base64EncryptedKey element in encrypted form")));
    signatureParts.add(submissionMetadata.getKey().getInstanceId());
    for (Path attachment : submissionMetadata.getAttachmentFilenames()) {
      if (!attachment.getFileName().toString().equals("submission.xml.enc")) {
        Path decryptedFile = submissionMetadata.getAttachmentFile(stripFileExtension(attachment.toString()));
        signatureParts.add(decryptedFile.getFileName() + "::" + getMd5Hash(decryptedFile).orElseThrow(BriefcaseException::new));
      }
    }
    Path submissionFile = submissionMetadata.getSubmissionFile();
    signatureParts.add(submissionFile.getFileName().toString() + "::" + getMd5Hash(submissionFile).orElseThrow(BriefcaseException::new));
    return String.join("\n", signatureParts) + "\n";
  }

  ParsedSubmission withPathAndDocument(Path path, Document document) {
    return new ParsedSubmission(submissionMetadata.withSubmissionFile(path), workingDir, XmlElement.of(document), validationStatus, cipherFactory, signature);
  }

  ParsedSubmission withValidationStatus(ValidationStatus validationStatus) {
    return new ParsedSubmission(submissionMetadata, workingDir, root, validationStatus, cipherFactory, signature);
  }


  /**
   * Gets this submission's next cipher object. This cipher should only be used to decrypt
   * a whole submission .enc file.
   *
   * <b>Warning</b>: This method has side-effects on {@link ParsedSubmission#cipherFactory}. Ciphers must
   * be retrieved in order to produce valid results.
   */
  // TODO Eagerly map a cipher to each element that must be decrypted to make the operation parallelizable
  Cipher getNextCipher() {
    return cipherFactory.map(CipherFactory::next).orElseThrow(() -> new BriefcaseException("No Cipher configured"));
  }

  public Optional<OffsetDateTime> getSubmissionDate() {
    return submissionMetadata.getSubmissionDateTime();
  }

  List<Path> getAttachmentFiles() {
    return submissionMetadata.getAttachmentFiles();
  }

  int countAttachments() {
    return submissionMetadata.getAttachmentFilenames().size();
  }

  Path getEncryptedXmlFile() {
    Path encryptedXmlFile = submissionMetadata.getEncryptedXmlFile();
    if (!Files.exists(encryptedXmlFile))
      throw new BriefcaseException("Encrypted file not found");
    return encryptedXmlFile;
  }

  public Optional<XmlElement> findFirstElement(String name) {
    return root.findFirstElement(name);
  }

  public ValidationStatus getValidationStatus() {
    return validationStatus;
  }

  public List<XmlElement> getDescendants(String fqn) {
    if (descendantsIndex == null)
      descendantsIndex = root.buildDescendantsIndex();
    return Optional.ofNullable(descendantsIndex.get(fqn)).orElse(emptyList());
  }

  /**
   * Validates this submission's decrypted signature using the provided signature
   */
  boolean isValid(byte[] signature) {
    return this.signature.map(s -> fastEquals(s, signature)).orElse(false);
  }

  /**
   * This is the custom, short-circuiting equals method for byte arrays that
   * was used originally. Since the arrays we need to compare are small, maybe
   * we could simply call Object.equals() on them.
   */
  // TODO Migrate to Objects.equals
  private static boolean fastEquals(byte[] left, byte[] right) {
    for (int i = 0, length = left.length; i < length; ++i)
      if (left[i] != right[i])
        return false;
    return true;
  }
}
