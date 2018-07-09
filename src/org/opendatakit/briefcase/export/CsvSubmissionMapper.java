package org.opendatakit.briefcase.export;

import static org.opendatakit.briefcase.export.CsvMapper.getMainSubmissionLines;
import static org.opendatakit.briefcase.export.CsvMapper.getRepeatCsvLines;

import java.time.OffsetDateTime;

/**
 * This functional interface describes the types of the operation that
 * takes a {@link Submission} and returns a {@link CsvLines}.
 * <p>
 * It also defines two factories, one for each kind of {@link Csv} output
 * Briefcase needs to generate.
 */
@FunctionalInterface
interface CsvSubmissionMapper {
  /**
   * This value will be used for {@link Submission} instances without submission date.
   * The idea is that these submissions should be the older than any other.
   */
  OffsetDateTime MIN_SUBMISSION_DATE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset());

  CsvLines apply(Submission submission);

  /**
   * Factory that will produce {@link CsvLines} corresponding to the main output file
   * of a form.
   */
  static CsvSubmissionMapper main(FormDefinition formDefinition, ExportConfiguration configuration, boolean exportMedia) {
    return submission -> CsvLines.of(
        formDefinition.getModel().fqn(),
        submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
        getMainSubmissionLines(
            submission,
            formDefinition.getModel(),
            formDefinition.isFileEncryptedForm(),
            exportMedia,
            configuration.getExportMediaPath()
        )
    );
  }

  /**
   * Factory that will produce {@link CsvLines} corresponding to any repeat output file
   * of a form.
   */
  static CsvSubmissionMapper repeat(Model groupModel, ExportConfiguration configuration, boolean exportMedia) {
    return submission -> CsvLines.of(
        groupModel.fqn(),
        submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
        getRepeatCsvLines(
            groupModel,
            submission.getElements(groupModel.fqn()),
            exportMedia,
            configuration.getExportMediaPath(),
            submission.getInstanceId(),
            submission.getWorkingDir()
        )
    );
  }
}
