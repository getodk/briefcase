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
  private final Optional<Path> formFile;
  private final Cursor cursor;
  private final Optional<OffsetDateTime> lastExportedSubmissionDate;

  public FormMetadata(FormKey key, Optional<Path> formFile, Cursor cursor, Optional<OffsetDateTime> lastExportedSubmissionDate) {
    this.key = key;
    this.formFile = formFile;
    this.cursor = cursor;
    this.lastExportedSubmissionDate = lastExportedSubmissionDate;
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
    return new FormMetadata(key, Optional.of(formFile), Cursor.empty(), Optional.empty());
  }

  private static boolean isTheMainInstance(XmlElement e) {
    return !e.hasAttribute("id") // It's not a secondary instance
        && e.childrenOf().size() == 1 // Has only one child (sanity check: an <instance> with more than one children is probably illegal)
        && e.childrenOf().get(0).hasAttribute("id"); // The only child has an id (sanity check: we can't handle forms without form id)
  }

  public static FormMetadata empty(FormKey key) {
    return new FormMetadata(key, Optional.empty(), Cursor.empty(), Optional.empty());
  }

  public FormKey getKey() {
    return key;
  }

  public Optional<Path> getFormFile() {
    return formFile;
  }

  public Cursor getCursor() {
    return cursor;
  }

  public Optional<OffsetDateTime> getLastExportedSubmissionDate() {
    return lastExportedSubmissionDate;
  }

  public FormMetadata withFormFile(Path formFile) {
    return new FormMetadata(key, Optional.of(formFile), cursor, lastExportedSubmissionDate);
  }

  FormMetadata withCursor(Cursor cursor) {
    return new FormMetadata(key, formFile, cursor, lastExportedSubmissionDate);
  }

  public FormMetadata withoutCursor() {
    return new FormMetadata(key, formFile, Cursor.empty(), lastExportedSubmissionDate);
  }

  FormMetadata withLastExportedSubmissionDate(OffsetDateTime submissionDate) {
    return new FormMetadata(key, formFile, cursor, Optional.of(submissionDate));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormMetadata that = (FormMetadata) o;
    return Objects.equals(key, that.key) &&
        Objects.equals(formFile, that.formFile) &&
        Objects.equals(cursor, that.cursor) &&
        Objects.equals(lastExportedSubmissionDate, that.lastExportedSubmissionDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, formFile, cursor, lastExportedSubmissionDate);
  }

  @Override
  public String toString() {
    return "FormMetadata{" +
        "key=" + key +
        ", formDir=" + formFile +
        ", cursor=" + cursor +
        ", lastExportedSubmissionDate=" + lastExportedSubmissionDate +
        '}';
  }
}
