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
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.opendatakit.briefcase.export.CipherFactory.signatureDecrypter;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.newInputStream;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
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
   * @param dateRange   a {@link DateRange} to include only the submissions that are contained in it
   * @return a {@link List} of {@link Submission} instances
   */
  static List<Submission> parseAllInFormDir(Path formDir, boolean isEncrypted, Optional<PrivateKey> privateKey, DateRange dateRange) {
    Path instancesDir = formDir.resolve("instances");
    if (!Files.exists(instancesDir) || !Files.isReadable(instancesDir))
      return Collections.emptyList();
    return walk(instancesDir).parallel()
        .filter(UncheckedFiles::isInstanceDir)
        // We must account for skipped submissions when they are missing
        // some media files or if they can't be decrypted. That's why, at
        // this point, we get an Optional<Submission> which we need to filter
        .map(path -> parseSubmission(path.resolve("submission.xml"), isEncrypted, privateKey))
        .filter(Optional::isPresent)
        .map(Optional::get)
        // Filter submissions that have been submitted in the given date range
        .filter(submission -> submission.metaData.submissionDate.map(dateRange::inRange).orElse(true))
        .collect(toList());
  }

  private static Optional<Submission> parseSubmission(Path path, boolean isEncrypted, Optional<PrivateKey> privateKey) {
    Path workingDir = isEncrypted ? createTempDirectory("briefcase") : path.getParent();
    return parse(path).flatMap(document -> {
      XmlElement root = XmlElement.of(document);
      SubmissionMetaData metaData = SubmissionMetaData.from(root);

      Optional<CipherFactory> cipherFactory = OptionalProduct.all(
          metaData.instanceId,
          metaData.base64EncryptedKey,
          privateKey
      ).flatMap(CipherFactory::from);

      Optional<byte[]> signature = OptionalProduct.all(
          privateKey,
          metaData.encryptedSignature
      ).map((pk, es) -> decrypt(signatureDecrypter(pk), decodeBase64(es)));

      Submission submission = Submission.notValidated(path, workingDir, root, metaData, cipherFactory, signature);
      if (isEncrypted)
        return submission.decrypt().map(decryptedSubmission ->
            decryptedSubmission.copy(ValidationStatus.of(isValid(submission, decryptedSubmission)))
        );
      return Optional.of(submission);
    });
  }

  private static Optional<Document> parse(Path submission) {
    try (InputStream is = newInputStream(submission);
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
    String computedSignature = decryptedSubmission.buildSignature(submission);
    return submission.signature
        .map(signature -> fastEquals(signature, computeDigest(computedSignature)))
        .orElse(false);
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
