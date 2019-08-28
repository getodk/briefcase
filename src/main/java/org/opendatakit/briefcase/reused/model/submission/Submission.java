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

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.model.submission.ValidationStatus.NOT_VALIDATED;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.XmlElement;

/**
 * This class represents a form's submission XML document.
 * <p>
 * All instances begin with a {@link ValidationStatus#NOT_VALIDATED} validation status,
 * which is only relevant if the form is encrypted.
 */
public class Submission {
  private final SubmissionMetadata submissionMetadata;
  private final XmlElement root;
  private final ValidationStatus validationStatus;
  private final Optional<CipherFactory> cipherFactory;
  private final Optional<byte[]> signature;
  /**
   * This index is lazily constructed
   *
   * @see #getElements(String)
   */
  private Map<String, List<XmlElement>> elementsByFqn;

  private Submission(SubmissionMetadata submissionMetadata, XmlElement root, ValidationStatus validationStatus, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    this.submissionMetadata = submissionMetadata;
    this.root = root;
    this.validationStatus = validationStatus;
    this.cipherFactory = cipherFactory;
    this.signature = signature;
  }

  public static Submission plain(SubmissionMetadata submissionMetadata, XmlElement root) {
    return new Submission(submissionMetadata, root, NOT_VALIDATED, Optional.empty(), Optional.empty());
  }

  static Submission unencrypted(SubmissionMetadata submissionMetadata, XmlElement root, CipherFactory cipherFactory, byte[] signature) {
    return new Submission(submissionMetadata, root, NOT_VALIDATED, Optional.of(cipherFactory), Optional.of(signature));
  }

  SubmissionMetadata getSubmissinoMetadata() {
    return submissionMetadata;
  }

  public Path getPath() {
    return submissionMetadata.getSubmissionFile();
  }

  public Path getWorkingDir() {
    return submissionMetadata.getSubmissionDir();
  }

  public String getInstanceId() {
    return submissionMetadata.getKey().getInstanceId();
  }

  /**
   * Builds a signature for this {@link Submission} instance's contents.
   * <p>
   * This method is used to validate the cryptographic signature attached to encrypted forms.
   *
   * @param originalSubmission the original, unencrypted {@link Submission} instance that was decrypted
   *                           to get this {@link Submission} instance
   * @return a {@link String} signature for this {@link Submission} instance
   */
  String buildSignature(Submission originalSubmission) {
    List<String> signatureParts = new ArrayList<>();
    signatureParts.add(submissionMetadata.getKey().getFormId());
    submissionMetadata.getKey().getFormVersion().ifPresent(signatureParts::add);
    signatureParts.add(submissionMetadata.getBase64EncryptedKey().orElseThrow(() -> new BriefcaseException("Missing base64EncryptedKey element in encrypted form")));
    signatureParts.add(submissionMetadata.getKey().getInstanceId());
    for (Path attachment : submissionMetadata.getAttachmentFilenames()) {
      Path decryptedFile = submissionMetadata.getSubmissionDir().resolve(stripFileExtension(attachment.toString()));
      signatureParts.add(decryptedFile.getFileName() + "::" + UncheckedFiles.getMd5Hash(decryptedFile).orElseThrow(BriefcaseException::new));
    }
    Path submissionFile = originalSubmission.submissionMetadata.getSubmissionFile();
    signatureParts.add(submissionFile.toString() + "::" + UncheckedFiles.getMd5Hash(submissionFile).orElseThrow(BriefcaseException::new));
    return String.join("\n", signatureParts) + "\n";
  }

  /**
   * Copies this instance replacing the value of the given arguments.
   * <p>
   * It will extract a new {@link XmlElement} root from the given {@link Document} document
   */
  Submission withPathAndDocument(Path path, Document document) {
    return new Submission(submissionMetadata.withSubmissionFile(path), XmlElement.of(document), validationStatus, cipherFactory, signature);
  }

  /**
   * Copies this instance replacing the value of the given arguments.
   *
   * @param validationStatus new {@link ValidationStatus} value
   * @return a new {@link Submission} instance
   */
  Submission withValidationStatus(ValidationStatus validationStatus) {
    return new Submission(submissionMetadata, root, validationStatus, cipherFactory, signature);
  }


  /**
   * Gets this submission's next {@link Cipher}. This cipher should only be used to decrypt
   * a whole submission .enc file.
   *
   * <b>Warning</b>: This method has side-effects on {@link Submission#cipherFactory}. Ciphers must
   * be retrieved in a certain order to produce valid results.
   */
  Cipher getNextCipher() {
    return cipherFactory.map(CipherFactory::next).orElseThrow(() -> new BriefcaseException("No Cipher configured"));
  }

  /**
   * Returns the submission date from this submission's meta data
   *
   * @return a {@link OffsetDateTime} with the submission date, wrapped in an {@link Optional},
   *     or an {@link Optional#empty()} if there is no submission date
   */
  public Optional<OffsetDateTime> getSubmissionDate() {
    return submissionMetadata.getSubmissionDateTime();
  }

  /**
   * Returns the {@link List} of {@link Path} paths to any media file attached to
   * this submission that is found along with the submission's xml file.
   *
   * @return a {@link List} of {@link Path} paths to attached media files
   */
  List<Path> getMediaPaths() {
    return submissionMetadata.getAttachmentFilenames().stream()
        .map(submissionMetadata::getAttachmentFile)
        .filter(Files::exists)
        .collect(toList());
  }

  /**
   * Returns the number of attached media files found with the submission's xml file.
   *
   * @return an {@link Integer} with the number of attached media files
   */
  int countMedia() {
    return submissionMetadata.getAttachmentFilenames().size();
  }

  Path getEncryptedFilePath() {
    return submissionMetadata.getEncryptedXmlFilename()
        .map(submissionMetadata::getAttachmentFile)
        .filter(path -> Files.exists(path))
        .orElseThrow(() -> new BriefcaseException("Encrypted file not found"));
  }

  /**
   * Returns the {@link XmlElement} with the given name among children of this
   * submission's root element.
   *
   * @param name the {@link String} name of the element to find
   * @return the {@link XmlElement} with the given name, wrapped in an {@link Optional},
   *     or {@link Optional#empty()} if no element with that name is found
   */
  public Optional<XmlElement> findElement(String name) {
    return root.findElement(name);
  }

  /**
   * Returns the {@link ValidationStatus} of this submission.
   *
   * @return the {@link ValidationStatus} of this submission
   */
  public ValidationStatus getValidationStatus() {
    return validationStatus;
  }

  /**
   * Returns a {@link List} of {@link XmlElement} elements with the same FQN among
   * children of this submission's root element.
   *
   * @param fqn the {@link String} fully qualified name to search for elements
   * @return a {@link List} of {@link XmlElement} elements with the same FQN
   */
  public List<XmlElement> getElements(String fqn) {
    if (elementsByFqn == null)
      elementsByFqn = root.getChildrenIndex();
    return Optional.ofNullable(elementsByFqn.get(fqn)).orElse(Collections.emptyList());
  }

  /**
   * Validates that the given signature equals the decrypted signature parsed
   * from this submission's xml file.
   *
   * @param signature a byte array containing the signature to be validated
   * @return true if the given signature is valid, false otherwise
   */
  boolean validate(byte[] signature) {
    return this.signature.map(s -> fastEquals(s, signature)).orElse(false);
  }

  /**
   * This is the custom, short-circuiting equals method for byte arrays that
   * was used originally. Since the arrays we need to compare are small, maybe
   * we could simply call Object.equals() on them.
   */
  private static boolean fastEquals(byte[] left, byte[] right) {
    for (int i = 0, length = left.length; i < length; ++i)
      if (left[i] != right[i])
        return false;
    return true;
  }
}
