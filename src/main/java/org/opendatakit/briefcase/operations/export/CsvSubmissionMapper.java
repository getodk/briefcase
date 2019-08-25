package org.opendatakit.briefcase.operations.export;

import org.opendatakit.briefcase.reused.model.submission.Submission;

/**
 * This functional interface represents the operation of transformation of a
 * {@link Submission} to a {@link CsvLines}.
 */
@FunctionalInterface
interface CsvSubmissionMapper {
  CsvLines apply(Submission submission);
}
