package org.opendatakit.briefcase.operations.export;

import org.opendatakit.briefcase.reused.model.submission.ParsedSubmission;

/**
 * This functional interface represents the operation of transformation of a
 * parsed submission to a CSV line
 */
@FunctionalInterface
interface CsvSubmissionMapper {
  CsvLines apply(ParsedSubmission submission);
}
