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
import static org.opendatakit.briefcase.export.ValidationStatus.NOT_VALIDATED;
import static org.opendatakit.briefcase.reused.UncheckedFiles.checksumOf;
import static org.opendatakit.briefcase.reused.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.util.FileSystemUtils.getMd5Hash;

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
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class represents a form's submission XML document.
 * <p>
 * All instances begin with a {@link ValidationStatus#NOT_VALIDATED} validation status,
 * which is only relevant if the form is encrypted.
 */
class Submission {
  private final Path path;
  private final Path workingDir;
  private final XmlElement root;
  private final SubmissionMetaData metaData;
  private final ValidationStatus validationStatus;
  private final Optional<CipherFactory> cipherFactory;
  private final Optional<byte[]> signature;
  /**
   * This index is lazily constructed
   *
   * @see #getElements(String)
   */
  private Map<String, List<XmlElement>> elementsByFqn;

  private Submission(Path path, Path workingDir, XmlElement root, SubmissionMetaData metaData, ValidationStatus validationStatus, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    this.path = path;
    this.workingDir = workingDir;
    this.root = root;
    this.metaData = metaData;
    this.validationStatus = validationStatus;
    this.cipherFactory = cipherFactory;
    this.signature = signature;
  }

  /**
   * All {@link Submission} instances have a {@link ValidationStatus#NOT_VALIDATED} validation status,
   * which is only OK if the form is not encrypted.
   *
   * @param path          the {@link Path} to this form's definition XML file
   * @param workingDir    the {@link Path} to the working directory where all filesystem operations should happen, if any
   * @param root          the root {@link XmlElement} of this submission
   * @param metaData      the {@link SubmissionMetaData} instance to read metadata from it
   * @param cipherFactory the {@link CipherFactory} instance, wrapped inside an {@link Optional}, or {@link Optional#empty()}
   *                      if the form requires no decryption
   * @param signature     the decoded cryptographic signature of the submission, wrapped inside an {@link Optional}, or {@link Optional#empty()}
   *                      if the form requires no decryption
   * @return a new {@link Submission} instance
   */
  static Submission notValidated(Path path, Path workingDir, XmlElement root, SubmissionMetaData metaData, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    return new Submission(path, workingDir, root, metaData, NOT_VALIDATED, cipherFactory, signature);
  }

  public Path getPath() {
    return path;
  }

  public boolean isValid(boolean formHasRepeatableFields) {
    return !formHasRepeatableFields || metaData.getInstanceId().isPresent();
  }

  /**
   * Returns the submission's instance ID.
   *
   * @return a {@link String} with the submission's instance ID
   * @throws BriefcaseException if there is no instance ID and the form has repeatable fields
   */
  String getInstanceId(boolean formHasRepeatableFields) {
    Optional<String> maybeInstanceId = metaData.getInstanceId();
    return formHasRepeatableFields
        ? maybeInstanceId.orElseThrow(() -> new BriefcaseException("The form has repeat groups and this submission has no instance ID"))
        : maybeInstanceId.orElse("");
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
    signatureParts.add(metaData.getFormId());
    metaData.getVersion().ifPresent(signatureParts::add);
    signatureParts.add(metaData.getBase64EncryptedKey().orElseThrow(() -> new ParsingException("Missing base64EncryptedKey element in encrypted form")));
    signatureParts.add(metaData.getInstanceId().orElseGet(() -> "crc32:" + checksumOf(originalSubmission.path)));
    for (String mediaName : metaData.getMediaNames()) {
      Path decryptedFile = workingDir.resolve(stripFileExtension(mediaName));
      signatureParts.add(decryptedFile.getFileName() + "::" + getMd5Hash(decryptedFile.toFile()));
    }
    signatureParts.add(originalSubmission.path.getFileName().toString() + "::" + getMd5Hash(path.toFile()));
    return String.join("\n", signatureParts) + "\n";
  }

  /**
   * Copies this instance replacing the value of the given arguments.
   * <p>
   * It will extract a new {@link XmlElement} root from the given {@link Document} document
   *
   * @param path     new {@link Path} path value
   * @param document a {@link Document} instance from which a new {@link XmlElement} root member will be taken
   * @return a new {@link Submission} instance
   */
  Submission copy(Path path, Document document) {
    return new Submission(path, workingDir, XmlElement.of(document), metaData, validationStatus, cipherFactory, signature);
  }

  /**
   * Copies this instance replacing the value of the given arguments.
   *
   * @param validationStatus new {@link ValidationStatus} value
   * @return a new {@link Submission} instance
   */
  Submission copy(ValidationStatus validationStatus) {
    return new Submission(path, workingDir, root, metaData, validationStatus, cipherFactory, signature);
  }


  /**
   * Gets this submission's next {@link Cipher}. This cipher should only be used to decrypt
   * a whole submission .enc file.
   *
   * <b>Warning</b>: This method has side-effects on {@link Submission#cipherFactory}. Ciphers must
   * be retrieved in a certain order to produce valid results.
   *
   * @return a new {@link Cipher} instance
   * @throws BriefcaseException if no CipherFactory is present
   * @see SubmissionParser#decrypt(Submission, SubmissionExportErrorCallback)
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
    return metaData.getSubmissionDate();
  }

  /**
   * Returns the working directory {@link Path} to be used in any filesystem operations.
   *
   * @return the {@link Path} to the working directory
   */
  Path getWorkingDir() {
    return workingDir;
  }

  /**
   * Returns the {@link List} of {@link Path} paths to any media file attached to
   * this submission that is found along with the submission's xml file.
   *
   * @return a {@link List} of {@link Path} paths to attached media files
   */
  List<Path> getMediaPaths() {
    return metaData.getMediaNames().stream()
        .map(fileName -> path.getParent().resolve(fileName))
        .filter(Files::exists)
        .collect(toList());
  }

  /**
   * Returns the number of attached media files found with the submission's xml file.
   *
   * @return an {@link Integer} with the number of attached media files
   */
  int countMedia() {
    return metaData.getMediaNames().size();
  }

  /**
   * Returns the {@link Path} to the related submission encrypted file.
   *
   * @return the {@link Path} to the submission encrypted file
   * @throws ParsingException if no encrypted file is found
   */
  Path getEncryptedFilePath() {
    return metaData.getEncryptedXmlFile()
        .map(fileName -> path.getParent().resolve(fileName))
        .filter(path -> Files.exists(path))
        .orElseThrow(() -> new ParsingException("Encrypted file not found"));
  }

  /**
   * Returns the {@link XmlElement} with the given name among children of this
   * submission's root element.
   *
   * @param name the {@link String} name of the element to find
   * @return the {@link XmlElement} with the given name, wrapped in an {@link Optional},
   *     or {@link Optional#empty()} if no element with that name is found
   */
  Optional<XmlElement> findElement(String name) {
    return root.findElement(name);
  }

  /**
   * Returns the {@link ValidationStatus} of this submission.
   *
   * @return the {@link ValidationStatus} of this submission
   */
  ValidationStatus getValidationStatus() {
    return validationStatus;
  }

  /**
   * Returns a {@link List} of {@link XmlElement} elements with the same FQN among
   * children of this submission's root element.
   *
   * @param fqn the {@link String} fully qualified name to search for elements
   * @return a {@link List} of {@link XmlElement} elements with the same FQN
   */
  List<XmlElement> getElements(String fqn) {
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
