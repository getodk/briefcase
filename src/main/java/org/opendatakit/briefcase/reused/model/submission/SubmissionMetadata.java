package org.opendatakit.briefcase.reused.model.submission;

import static java.nio.file.Files.isRegularFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Iso8601Helpers;
import org.opendatakit.briefcase.reused.model.XmlElement;

public class SubmissionMetadata {
  private final SubmissionKey submissionKey;
  private final Optional<Path> submissionFile;
  private final Optional<OffsetDateTime> submissionDateTime;
  private final Optional<Path> encryptedXmlFilename;
  private final Optional<String> base64EncryptedKey;
  private final Optional<String> encryptedSignature;
  private final List<Path> attachmentFilenames;

  public SubmissionMetadata(SubmissionKey submissionKey, Optional<Path> submissionFile, Optional<OffsetDateTime> submissionDateTime, Optional<Path> encryptedXmlFilename, Optional<String> base64EncryptedKey, Optional<String> encryptedSignature, List<Path> attachmentFilenames) {
    this.submissionKey = submissionKey;
    this.submissionFile = submissionFile;
    this.submissionDateTime = submissionDateTime;
    this.encryptedXmlFilename = encryptedXmlFilename;
    this.base64EncryptedKey = base64EncryptedKey;
    this.encryptedSignature = encryptedSignature;
    this.attachmentFilenames = attachmentFilenames;
  }

  public static SubmissionMetadata from(Path submissionFile, List<Path> attachmentFilenames) {
    XmlElement root = XmlElement.from(submissionFile);
    return new SubmissionMetadata(
        SubmissionKey.from(root),
        Optional.of(submissionFile),
        extractSubmissionDateTime(root),
        extractEncryptedXmlFile(root),
        extractBase64EncryptedKey(root),
        extractEncryptedSignature(root),
        attachmentFilenames
    );
  }

  public static SubmissionMetadata from(Path submissionFile, String instanceId, List<Path> attachmentFilenames) {
    XmlElement root = XmlElement.from(submissionFile);
    return new SubmissionMetadata(
        SubmissionKey.from(root, instanceId),
        Optional.of(submissionFile),
        extractSubmissionDateTime(root),
        extractEncryptedXmlFile(root),
        extractBase64EncryptedKey(root),
        extractEncryptedSignature(root),
        attachmentFilenames
    );
  }

  private static Optional<String> extractBase64EncryptedKey(XmlElement root) {
    return root.findElement("base64EncryptedKey").flatMap(XmlElement::maybeValue);
  }

  private static Optional<OffsetDateTime> extractSubmissionDateTime(XmlElement root) {
    return root.getAttributeValue("submissionDate").map(Iso8601Helpers::parseDateTime);
  }

  private static Optional<String> extractEncryptedSignature(XmlElement root) {
    return root.findElement("base64EncryptedElementSignature").flatMap(XmlElement::maybeValue);
  }

  private static Optional<Path> extractEncryptedXmlFile(XmlElement root) {
    return root.findElement("encryptedXmlFile").flatMap(XmlElement::maybeValue).map(Paths::get);
  }

  static boolean hasInstanceId(Path submissionFile) {
    return SubmissionKey.extractInstanceId(XmlElement.from(submissionFile)).isPresent();
  }

  static boolean isSubmissionFile(Path file) {
    return isRegularFile(file) && file.getFileName().toString().equals("submission.xml");
  }

  public SubmissionKey getKey() {
    return submissionKey;
  }

  public Path getSubmissionFile() {
    return submissionFile.orElseThrow(BriefcaseException::new);
  }

  public Path getSubmissionDir() {
    return getSubmissionFile().getParent();
  }

  Path getAttachmentFile(Path attachment) {
    return getSubmissionDir().resolve(attachment.getFileName());
  }

  Optional<OffsetDateTime> getSubmissionDateTime() {
    return submissionDateTime;
  }

  Optional<Path> getEncryptedXmlFilename() {
    return encryptedXmlFilename;
  }

  Optional<String> getBase64EncryptedKey() {
    return base64EncryptedKey;
  }

  Optional<String> getEncryptedSignature() {
    return encryptedSignature;
  }

  List<Path> getAttachmentFilenames() {
    return attachmentFilenames;
  }

  public SubmissionMetadata withSubmissionFile(Path submissionFile) {
    return new SubmissionMetadata(submissionKey, Optional.of(submissionFile), submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }

  SubmissionMetadata withAttachmentFilenames(List<Path> attachmentFilenames) {
    return new SubmissionMetadata(submissionKey, submissionFile, submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }
}
