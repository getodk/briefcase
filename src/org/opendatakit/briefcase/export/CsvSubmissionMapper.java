package org.opendatakit.briefcase.export;

/**
 * This functional interface represents the operation of transformation of a
 * {@link Submission} to a {@link CsvLines}.
 */
@FunctionalInterface
interface CsvSubmissionMapper {
  CsvLines apply(Submission submission);
}
