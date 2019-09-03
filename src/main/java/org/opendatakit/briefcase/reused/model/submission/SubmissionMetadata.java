package org.opendatakit.briefcase.reused.model.submission;

import static java.nio.file.Files.isRegularFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Iso8601Helpers;
import org.opendatakit.briefcase.reused.model.XmlElement;

public class SubmissionMetadata {
  public static final OffsetDateTime MIN_SUBMISSION_DATE_TIME = OffsetDateTime.parse("1970-01-01T00:00:00.000Z");

  private final SubmissionKey submissionKey;
  private final Optional<Path> submissionFile;
  private final Optional<OffsetDateTime> submissionDateTime;
  private final Optional<Path> encryptedXmlFilename;
  private final Optional<String> base64EncryptedKey;
  // TODO Rename this to base64EncryptedSignature
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
    return root.findFirstElement("base64EncryptedKey").flatMap(XmlElement::maybeValue);
  }

  private static Optional<OffsetDateTime> extractSubmissionDateTime(XmlElement root) {
    return root.getAttributeValue("submissionDate").map(Iso8601Helpers::parseDateTime);
  }

  private static Optional<String> extractEncryptedSignature(XmlElement root) {
    return root.findFirstElement("base64EncryptedElementSignature").flatMap(XmlElement::maybeValue);
  }

  private static Optional<Path> extractEncryptedXmlFile(XmlElement root) {
    return root.findFirstElement("encryptedXmlFile").flatMap(XmlElement::maybeValue).map(Paths::get);
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

  Optional<OffsetDateTime> getSubmissionDateTime() {
    return submissionDateTime;
  }

  Optional<Path> getEncryptedXmlFilename() {
    return encryptedXmlFilename;
  }

  Path getEncryptedXmlFile() {
    return getSubmissionDir().resolve(getEncryptedXmlFilename().orElseThrow(BriefcaseException::new));
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

  List<Path> getAttachmentFiles() {
    Path submissionDir = getSubmissionDir();
    return attachmentFilenames.stream()
        .map(submissionDir::resolve)
        .collect(Collectors.toList());
  }

  public Path getAttachmentFile(String filename) {
    return getSubmissionDir().resolve(filename);
  }

  public SubmissionMetadata withSubmissionFile(Path submissionFile) {
    return new SubmissionMetadata(submissionKey, Optional.of(submissionFile), submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SubmissionMetadata that = (SubmissionMetadata) o;
    return Objects.equals(submissionKey, that.submissionKey) &&
        Objects.equals(submissionFile, that.submissionFile) &&
        Objects.equals(submissionDateTime, that.submissionDateTime) &&
        Objects.equals(encryptedXmlFilename, that.encryptedXmlFilename) &&
        Objects.equals(base64EncryptedKey, that.base64EncryptedKey) &&
        Objects.equals(encryptedSignature, that.encryptedSignature) &&
        Objects.equals(attachmentFilenames, that.attachmentFilenames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionKey, submissionFile, submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }

  @Override
  public String toString() {
    return "SubmissionMetadata{" +
        "submissionKey=" + submissionKey +
        ", submissionFile=" + submissionFile +
        ", submissionDateTime=" + submissionDateTime +
        ", encryptedXmlFilename=" + encryptedXmlFilename +
        ", base64EncryptedKey=" + base64EncryptedKey +
        ", encryptedSignature=" + encryptedSignature +
        ", attachmentFilenames=" + attachmentFilenames +
        '}';
  }
}
