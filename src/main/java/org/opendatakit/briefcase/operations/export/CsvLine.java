package org.opendatakit.briefcase.operations.export;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * This class represents a CSV file line. It holds the submission date in case
 * this line should be sorted.
 * <p>
 * Instances of this class are Value Objects:
 * - Immutable
 * - Value equlity rules
 */
class CsvLine {
  private final String instanceId;
  private final OffsetDateTime submissionDate;
  private final String line;

  CsvLine(String instanceId, OffsetDateTime submissionDate, String line) {
    this.instanceId = instanceId;
    this.submissionDate = submissionDate;
    this.line = line;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public OffsetDateTime getSubmissionDate() {
    return submissionDate;
  }

  public String getLine() {
    return line;
  }

  @Override
  public String toString() {
    return "csv[" + instanceId + " - " + submissionDate + "] " + line + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CsvLine csvLine = (CsvLine) o;
    return Objects.equals(submissionDate, csvLine.submissionDate) &&
        Objects.equals(line, csvLine.line);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionDate, line);
  }
}
