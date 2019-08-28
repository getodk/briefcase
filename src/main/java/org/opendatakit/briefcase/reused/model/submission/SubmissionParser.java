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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.model.submission.CipherFactory.signatureDecrypter;
import static org.opendatakit.briefcase.reused.model.submission.ValidationStatus.NOT_VALID;
import static org.opendatakit.briefcase.reused.model.submission.ValidationStatus.VALID;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SubmissionParser {
  private static final Logger log = LoggerFactory.getLogger(SubmissionParser.class);

  public static Optional<ParsedSubmission> parsePlainSubmission(SubmissionMetadata submissionMetadata, BiConsumer<SubmissionMetadata, String> onError) {
    return parse(submissionMetadata, onError)
        .map(document -> ParsedSubmission.plain(submissionMetadata, XmlElement.of(document)));
  }

  public static Optional<ParsedSubmission> parseEncryptedSubmission(SubmissionMetadata submissionMetadata, PrivateKey privateKey, BiConsumer<SubmissionMetadata, String> onError) {
    return parse(submissionMetadata, onError).flatMap(document -> {
      ParsedSubmission submission = ParsedSubmission.unencrypted(
          submissionMetadata,
          XmlElement.of(document),
          CipherFactory.from(
              submissionMetadata.getKey().getInstanceId(),
              submissionMetadata.getBase64EncryptedKey().orElseThrow(BriefcaseException::new),
              privateKey
          ),
          decrypt(
              signatureDecrypter(privateKey),
              decodeBase64(submissionMetadata.getEncryptedSignature().orElseThrow(BriefcaseException::new))
          )
      );
      return decrypt(submission, onError)
          .map(s -> s.withValidationStatus(isValid(submission, s)));
    });
  }

  private static Optional<ParsedSubmission> decrypt(ParsedSubmission submission, BiConsumer<SubmissionMetadata, String> onError) {
    List<Path> attachmentFiles = submission.getAttachmentFiles();

    if (attachmentFiles.size() != submission.countAttachments())
      // We must skip this submission because some media file is missing
      return Optional.empty();

    // Decrypt each attached media file in order
    attachmentFiles.stream()
        .filter(Files::exists)
        .forEach(path -> decryptFile(path, submission.getWorkingDir(), submission.getNextCipher()));

    // Decrypt the submission
    Path decryptedSubmission = decryptFile(submission.getEncryptedXmlFile(), submission.getWorkingDir(), submission.getNextCipher());

    // Parse the document and, if everything goes well, return a decripted copy of the submission
    return parse(decryptedSubmission, submission.getSubmissinoMetadata(), onError)
        .map(document -> submission.withPathAndDocument(decryptedSubmission, document));
  }

  private static Path decryptFile(Path encFile, Path workingDir, Cipher cipher) {
    Path decryptedFile = workingDir.resolve(stripFileExtension(encFile));
    try (InputStream is = Files.newInputStream(encFile);
         CipherInputStream cis = new CipherInputStream(is, cipher);
         OutputStream os = Files.newOutputStream(decryptedFile)) {
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

  private static Optional<Document> parse(SubmissionMetadata submissionMetadata, BiConsumer<SubmissionMetadata, String> onError) {
    return parse(submissionMetadata.getSubmissionFile(), submissionMetadata, onError);
  }

  private static Optional<Document> parse(Path submissionFile, SubmissionMetadata submissionMetadata, BiConsumer<SubmissionMetadata, String> onError) {
    try (InputStream is = Files.newInputStream(submissionFile);
         InputStreamReader isr = new InputStreamReader(is, UTF_8)) {
      Document tempDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      tempDoc.parse(parser);
      return Optional.of(tempDoc);
    } catch (IOException | XmlPullParserException e) {
      log.error("Can't parse submission", e);
      onError.accept(submissionMetadata, "parsing error");
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

  private static ValidationStatus isValid(ParsedSubmission submission, ParsedSubmission decryptedSubmission) {
    return submission.isValid(computeDigest(decryptedSubmission.buildSignature(submission))) ? VALID : NOT_VALID;
  }

  private static byte[] computeDigest(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(message.getBytes(UTF_8));
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new CryptoException("Can't compute digest", e);
    }
  }
}
