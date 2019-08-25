package org.opendatakit.briefcase.reused.model.submission;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

  public SubmissionMetadata withSubmissionFile(Path submissionFile) {
    return new SubmissionMetadata(submissionKey, Optional.of(submissionFile), submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }

  public SubmissionKey getKey() {
    return submissionKey;
  }

  public Optional<Path> getSubmissionFile() {
    return submissionFile;
  }

  public Optional<OffsetDateTime> getSubmissionDateTime() {
    return submissionDateTime;
  }

  public Optional<Path> getEncryptedXmlFilename() {
    return encryptedXmlFilename;
  }

  public Optional<String> getBase64EncryptedKey() {
    return base64EncryptedKey;
  }

  public Optional<String> getEncryptedSignature() {
    return encryptedSignature;
  }

  public List<Path> getAttachmentFilenames() {
    return attachmentFilenames;
  }

  public SubmissionMetadata withAttachmentFilenames(List<Path> attachmentFilenames) {
    return new SubmissionMetadata(submissionKey, submissionFile, submissionDateTime, encryptedXmlFilename, base64EncryptedKey, encryptedSignature, attachmentFilenames);
  }
}
