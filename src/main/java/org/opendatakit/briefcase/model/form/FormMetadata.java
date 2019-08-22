package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class FormMetadata {
  private final FormKey key;
  private final Path formDir;
  private final Path formFilename;
  private final boolean hasBeenPulled;
  private final Cursor cursor;
  private final Optional<OffsetDateTime> lastExportedSubmissionDate;

  public FormMetadata(FormKey key, Path formDir, Path formFilename, boolean hasBeenPulled, Cursor cursor, Optional<OffsetDateTime> lastExportedSubmissionDate) {
    this.key = key;
    this.formDir = formDir;
    this.formFilename = formFilename;
    this.hasBeenPulled = hasBeenPulled;
    this.cursor = cursor;
    this.lastExportedSubmissionDate = lastExportedSubmissionDate;
  }

  public static FormMetadata of(FormKey key, Path storageDirectory, Path formFilename) {
    // Hardcoded storage directory because we want this class to decide where a
    // form is/should be stored. Now it's based on the Briefcase storage directory,
    // but it will change in the future to be based on a hash of the form key.
    return new FormMetadata(key, storageDirectory, formFilename, false, Cursor.empty(), Optional.empty());
  }

  public static FormMetadata from(Path formFile) {
    XmlElement root = XmlElement.from(formFile);
    assert root.getName().equals("html");
    String name = root.findElements("head", "title").get(0)
        .maybeValue()
        .orElseThrow(BriefcaseException::new);
    XmlElement mainInstance = root.findElements("head", "model", "instance").stream()
        .filter(FormMetadata::isTheMainInstance)
        .findFirst()
        .orElseThrow(BriefcaseException::new);
    String id = mainInstance.childrenOf().get(0).getAttributeValue("id").orElseThrow(BriefcaseException::new);
    Optional<String> version = mainInstance.childrenOf().get(0).getAttributeValue("version");
    FormKey key = FormKey.of(name, id, version);
    return new FormMetadata(key, formFile.getParent(), formFile.getFileName(), true, Cursor.empty(), Optional.empty());
  }

  private static boolean isTheMainInstance(XmlElement e) {
    return !e.hasAttribute("id") // It's not a secondary instance
        && e.childrenOf().size() == 1 // Has only one child (sanity check: an <instance> with more than one children is probably illegal)
        && e.childrenOf().get(0).hasAttribute("id"); // The only child has an id (sanity check: we can't handle forms without form id)
  }

  public FormKey getKey() {
    return key;
  }

  public Path getFormDir() {
    return formDir;
  }

  public Path getFormFilename() {
    return formFilename;
  }

  public boolean hasBeenPulled() {
    return hasBeenPulled;
  }

  public Cursor getCursor() {
    return cursor;
  }

  public Optional<OffsetDateTime> getLastExportedSubmissionDate() {
    return lastExportedSubmissionDate;
  }

  FormMetadata withCursor(Cursor cursor) {
    return new FormMetadata(key, formDir, formFilename, hasBeenPulled, cursor, lastExportedSubmissionDate);
  }

  public FormMetadata withoutCursor() {
    return new FormMetadata(key, formDir, formFilename, hasBeenPulled, Cursor.empty(), lastExportedSubmissionDate);
  }

  FormMetadata withHasBeenPulled(boolean hasBeenPulled) {
    return new FormMetadata(key, formDir, formFilename, hasBeenPulled, cursor, lastExportedSubmissionDate);
  }

  FormMetadata withLastExportedSubmissionDate(OffsetDateTime submissionDate) {
    return new FormMetadata(key, formDir, formFilename, hasBeenPulled, cursor, Optional.of(submissionDate));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormMetadata that = (FormMetadata) o;
    return hasBeenPulled == that.hasBeenPulled &&
        Objects.equals(key, that.key) &&
        Objects.equals(formDir, that.formDir) &&
        Objects.equals(formFilename, that.formFilename) &&
        Objects.equals(cursor, that.cursor) &&
        Objects.equals(lastExportedSubmissionDate, that.lastExportedSubmissionDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, formDir, formFilename, hasBeenPulled, cursor, lastExportedSubmissionDate);
  }

  @Override
  public String toString() {
    return "FormMetadata{" +
        "key=" + key +
        ", formDir=" + formDir +
        ", formFilename=" + formFilename +
        ", hasBeenPulled=" + hasBeenPulled +
        ", cursor=" + cursor +
        ", lastExportedSubmissionDate=" + lastExportedSubmissionDate +
        '}';
  }
}
