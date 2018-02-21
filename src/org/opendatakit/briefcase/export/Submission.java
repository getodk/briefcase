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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.ValidationStatus.NOT_VALIDATED;
import static org.opendatakit.briefcase.reused.UncheckedFiles.checksumOf;
import static org.opendatakit.briefcase.reused.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.util.FileSystemUtils.getMd5Hash;
import static org.opendatakit.briefcase.util.XmlManipulationUtils.parseXml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class represents a form submission XML document.
 * <p>
 * All instances begin with a {@link ValidationStatus#NOT_VALIDATED} validation status,
 * which is only relevant if the form is encrypted.
 */
class Submission {
  final Path path;
  final Path workingDir;
  final XmlElement root;
  final SubmissionMetaData metaData;
  final ValidationStatus validationStatus;
  private final Optional<CipherFactory> cipherFactory;
  final Optional<byte[]> signature;

  private Submission(Path path, Path workingDir, XmlElement root, SubmissionMetaData metaData, ValidationStatus validationStatus, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    this.path = path;
    this.workingDir = workingDir;
    this.root = root;
    this.metaData = metaData;
    this.validationStatus = validationStatus;
    this.cipherFactory = cipherFactory;
    this.signature = signature;
  }

  static Submission notValidated(Path path, Path workingDir, XmlElement root, SubmissionMetaData metaData, Optional<CipherFactory> cipherFactory, Optional<byte[]> signature) {
    return new Submission(
        path,
        workingDir,
        root,
        metaData,
        NOT_VALIDATED,
        cipherFactory,
        signature
    );
  }

  /**
   * Return the submission's instance ID.
   * <p>
   * This method will throw a {@link BriefcaseException} if the submission has no instance ID
   *
   * @return a {@link String} with the submission's instance ID
   */
  String getInstanceId() {
    return metaData.instanceId.orElseThrow(() -> new BriefcaseException("No instance ID found"));
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
    signatureParts.add(metaData.formId);
    metaData.version.ifPresent(signatureParts::add);
    signatureParts.add(metaData.base64EncryptedKey.orElseThrow(() -> new ParsingException("Missing base64EncryptedKey element in encrypted form")));
    signatureParts.add(metaData.instanceId.orElseGet(() -> "crc32:" + checksumOf(originalSubmission.path)));
    for (String mediaName : metaData.mediaNames) {
      Path decryptedFile = workingDir.resolve(stripFileExtension(mediaName));
      signatureParts.add(decryptedFile.getFileName() + "::" + getMd5Hash(decryptedFile.toFile()));
    }
    signatureParts.add(originalSubmission.path.getFileName().toString() + "::" + getMd5Hash(path.toFile()));
    return signatureParts.stream().collect(joining("\n")) + "\n";
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
   * Decrypts this submission and the media files attached to it.
   *
   * @return a new {@link Submission} decypted instance wrapped inside an {@link Optional}
   *     instance if everything goes well. An {@link Optional#empty()} otherwise
   */
  Optional<Submission> decrypt() {
    List<Path> mediaPaths = metaData.mediaNames.stream()
        .map(fileName -> path.getParent().resolve(fileName))
        .filter(Files::exists)
        .collect(toList());

    if (mediaPaths.size() != metaData.mediaNames.size())
      // We must skip this submission because some media file is missing
      return Optional.empty();

    mediaPaths.forEach(path -> decrypt(path, workingDir, getCipher()));

    Path encryptedFile = metaData.encriptedSubmissionFileName
        .map(fileName -> path.getParent().resolve(fileName))
        .filter(path -> Files.exists(path))
        .orElseThrow(() -> new ParsingException("Encrypted file not found"));

    Path decryptedFile = decrypt(encryptedFile, workingDir, getCipher());

    return Optional.ofNullable(parseXml(decryptedFile.toFile()))
        .map(document -> copy(decryptedFile, document));
  }

  private static Path decrypt(Path encFile, Path workingDir, Cipher cipher) {
    if (!encFile.getFileName().toString().endsWith(".enc"))
      throw new IllegalArgumentException("Encrypted file must have .enc extension");

    Path decryptedFile = workingDir.resolve(stripFileExtension(encFile.getFileName().toString()));
    try (InputStream is = Files.newInputStream(encFile);
         CipherInputStream cis = new CipherInputStream(is, cipher);
         OutputStream os = Files.newOutputStream(decryptedFile)
    ) {
      byte[] buffer = new byte[2048];
      int len = cis.read(buffer);
      while (len != -1) {
        os.write(buffer, 0, len);
        len = cis.read(buffer);
      }
      os.flush();
      return decryptedFile;
    } catch (IOException e) {
      throw new CryptoException("Can't decrypt file", e);
    }
  }

  private Cipher getCipher() {
    return cipherFactory.map(CipherFactory::getSubmissionCipher).orElseThrow(() -> new BriefcaseException("No Cipher configured"));
  }

}
