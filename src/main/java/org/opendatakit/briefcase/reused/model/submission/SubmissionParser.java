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
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.model.submission.CipherFactory.signatureDecrypter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
  private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

  /**
   * Returns a parsed submission. The result will be a non-empty Optional when:
   * <ul>
   * <li>The file at the given path can be parsed</li>
   * <li>If the form is encrypted, the submission can be decrypted</li>
   * </ul>
   * <p>
   * Otherwise, it returns an empty Optional without throwing any exception.
   */
  public static Optional<Submission> parsePlainSubmission(SubmissionMetadata submissionMetadata, BiConsumer<SubmissionMetadata, String> onError) {
    return parse(submissionMetadata, onError).flatMap(document -> {
      XmlElement root = XmlElement.of(document);
      return Optional.of(Submission.plain(submissionMetadata, root));
    });
  }

  public static Optional<Submission> parseEncryptedSubmission(SubmissionMetadata submissionMetadata, PrivateKey privateKey, BiConsumer<SubmissionMetadata, String> onError) {
    return parse(submissionMetadata, onError).flatMap(document -> {
      XmlElement root = XmlElement.of(document);
      SubmissionLazyMetadata metaData = new SubmissionLazyMetadata(root);

      CipherFactory cipherFactory = CipherFactory.from(
          metaData.getInstanceId().orElseThrow(BriefcaseException::new),
          metaData.getBase64EncryptedKey().orElseThrow(BriefcaseException::new),
          privateKey
      );

      // If all the needed parts are present, decrypt the signature
      byte[] signature = decrypt(
          signatureDecrypter(privateKey),
          decodeBase64(metaData.getEncryptedSignature().orElseThrow(BriefcaseException::new))
      );

      Submission submission = Submission.unencrypted(submissionMetadata, root, cipherFactory, signature);
      return decrypt(submission, onError).map(s -> s.withValidationStatus(ValidationStatus.of(isValid(submission, s))));
    });
  }

  private static Optional<String> parseAttribute(Path submission, Reader ioReader, String attributeName, BiConsumer<Path, String> onParsingError) {
    try {
      XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(ioReader);
      while (reader.hasNext())
        if (reader.next() == START_ELEMENT)
          for (int i = 0, c = reader.getAttributeCount(); i < c; ++i)
            if (reader.getAttributeLocalName(i).equals(attributeName))
              return Optional.of(reader.getAttributeValue(i));
    } catch (XMLStreamException e) {
      log.error("Can't parse submission", e);
      onParsingError.accept(submission, "parsing error");
    }
    return Optional.empty();
  }


  private static Optional<Submission> decrypt(Submission submission, BiConsumer<SubmissionMetadata, String> onError) {
    List<Path> mediaPaths = submission.getMediaPaths();

    if (mediaPaths.size() != submission.countMedia())
      // We must skip this submission because some media file is missing
      return Optional.empty();

    // Decrypt each attached media file in order
    mediaPaths.forEach(path -> decryptFile(path, submission.getWorkingDir(), submission.getNextCipher()));

    // Decrypt the submission
    Path decryptedSubmission = decryptFile(submission.getEncryptedFilePath(), submission.getWorkingDir(), submission.getNextCipher());

    // Parse the document and, if everything goes well, return a decripted copy of the submission
    return parse(decryptedSubmission, submission.getSubmissinoMetadata(), onError)
        .map(document -> submission.withPathAndDocument(decryptedSubmission, document));
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

  private static boolean isValid(Submission submission, Submission decryptedSubmission) {
    return submission.validate(computeDigest(decryptedSubmission.buildSignature(submission)));
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
