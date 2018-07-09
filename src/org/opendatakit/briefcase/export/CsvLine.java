package org.opendatakit.briefcase.export;

import java.time.OffsetDateTime;

/**
 * This class represents a CSV file line. It holds the submission date in case
 * this line should be sorted.
 */
class CsvLine {
  private final OffsetDateTime submissionDate;
  private final String line;

  CsvLine(OffsetDateTime submissionDate, String line) {
    this.submissionDate = submissionDate;
    this.line = line;
  }

  public OffsetDateTime getSubmissionDate() {
    return submissionDate;
  }

  public String getLine() {
    return line;
  }
}
