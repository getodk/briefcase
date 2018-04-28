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

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.opendatakit.briefcase.export.CipherFactory.signatureDecrypter;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.stripFileExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This class holds the main submission parsing code.
 */
class SubmissionParser {
  private static final Logger log = LoggerFactory.getLogger(SubmissionParser.class);

  /**
   * Parses all submission files in a form's directory and returns a {@link List} of {@link Submission} instances.
   *
   * @param formDir     the {@link Path} directory of the form
   * @param isEncrypted a {@link Boolean} indicating if the form is encrypted or not
   * @param privateKey  {@link PrivateKey} instance wrapped inside an {@link Optional} instance
   * @param dateRange   a {@link DateRange} to filter submissions that are contained in it
   * @return a {@link List} of {@link Submission} instances
   */
  static Stream<Submission> parseAllInFormDir(Path formDir, boolean isEncrypted, Optional<PrivateKey> privateKey, DateRange dateRange) {
    Path instancesDir = formDir.resolve("instances");
    if (!Files.exists(instancesDir) || !Files.isReadable(instancesDir))
      return Stream.empty();

    return list(instancesDir).parallel()
        // Normally, we would use the Streams API to filter out files that
        // are not contained in the given date range, but parsing is expensive
        // enough to make it all in one pass and use Optionals to account
        // for that
        .filter(UncheckedFiles::isInstanceDir)
        .map(path -> parseSubmission(path.resolve("submission.xml"), dateRange, isEncrypted, privateKey))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private static Optional<Submission> parseSubmission(Path path, DateRange dateRange, boolean isEncrypted, Optional<PrivateKey> privateKey) {
    Path workingDir = isEncrypted ? createTempDirectory("briefcase") : path.getParent();
    return parse(path).flatMap(document -> {
      XmlElement root = XmlElement.of(document);
      SubmissionMetaData metaData = new SubmissionMetaData(root);

      // Filter out the submission if it's not included in the given date range
      if (!metaData.getSubmissionDate().map(dateRange::contains).orElse(false))
        return Optional.empty();

      // If all the needed parts are present, prepare the CipherFactory instance
      Optional<CipherFactory> cipherFactory = OptionalProduct.all(
          metaData.getInstanceId(),
          metaData.getBase64EncryptedKey(),
          privateKey
      ).map(CipherFactory::from);

      // If all the needed parts are present, decrypt the signature
      Optional<byte[]> signature = OptionalProduct.all(
          privateKey,
          metaData.getEncryptedSignature()
      ).map((pk, es) -> decrypt(signatureDecrypter(pk), decodeBase64(es)));

      Submission submission = Submission.notValidated(path, workingDir, root, metaData, cipherFactory, signature);
      return isEncrypted
          // If it's encrypted, validate the parsed contents with the attached signature
          ? decrypt(submission).map(s -> s.copy(ValidationStatus.of(isValid(submission, s))))
          // Return the original submission otherwise
          : Optional.of(submission);
    });
  }

  private static Optional<Submission> decrypt(Submission submission) {
    List<Path> mediaPaths = submission.getMediaPaths();

    if (mediaPaths.size() != submission.countMedia())
      // We must skip this submission because some media file is missing
      return Optional.empty();

    // Decrypt each attached media file in order
    mediaPaths.forEach(path -> decryptFile(path, submission.getWorkingDir(), submission.getNextCipher()));

    // Decrypt the submission
    Path decryptedSubmission = decryptFile(submission.getEncryptedFilePath(), submission.getWorkingDir(), submission.getNextCipher());

    // Parse the document and, if everything goes well, return a decripted copy of the submission
    return parse(decryptedSubmission).map(document -> submission.copy(decryptedSubmission, document));
  }

  private static Path decryptFile(Path encFile, Path workingDir, Cipher cipher) {
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

  private static Optional<Document> parse(Path submission) {
    try (InputStream is = Files.newInputStream(submission);
         InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
      Document tempDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      tempDoc.parse(parser);
      return Optional.of(tempDoc);
    } catch (IOException | XmlPullParserException e) {
      log.warn("Can't parse submission {} {}", submission.getParent().getParent().getParent().getFileName(), submission.getParent().getFileName(), e);
      return Optional.empty();
    }
  }

  @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
  private static byte[] decrypt(Cipher cipher, byte[] message) {
    try {
      return cipher.doFinal(message);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CryptoException("Can't decrypt message");
    }
  }

  private static boolean isValid(Submission submission, Submission decryptedSubmission) {
    return submission.validate(computeDigest(decryptedSubmission.buildSignature(submission)));
  }

  private static byte[] computeDigest(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(message.getBytes("UTF-8"));
      return md.digest();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new CryptoException("Can't compute digest", e);
    }
  }
}
