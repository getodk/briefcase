package org.opendatakit.briefcase.reused.model.submission;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.walk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class SubmissionMetadataCommands {
  public static Consumer<SubmissionMetadataPort> insert(SubmissionMetadata submissionMetadata) {
    return port -> port.persist(submissionMetadata);
  }

  /**
   * Finds all submissions present in the filesystem that belong to the
   * provided collection of forms and stores them.
   */
  public static Consumer<SubmissionMetadataPort> syncSubmissions(Stream<FormMetadata> formMetadataStream) {
    return port -> {
      port.flush();

      Stream<SubmissionMetadata> submissionMetadataStream = formMetadataStream
          .filter(formMetadata -> Files.exists(formMetadata.getSubmissionsDir()))
          .flatMap(SubmissionMetadataCommands::getFilesystemSubmissions);

      port.persist(submissionMetadataStream);
    };
  }

  private static Stream<SubmissionMetadata> getFilesystemSubmissions(FormMetadata formMetadata) {
    return walk(formMetadata.getSubmissionsDir())
        .filter(SubmissionMetadata::isSubmissionFile)
        .filter(SubmissionMetadata::hasInstanceId)
        .map(submissionFile -> SubmissionMetadata.from(
            submissionFile,
            list(submissionFile.getParent())
                .filter(not(SubmissionMetadata::isSubmissionFile))
                .map(Path::getFileName)
                .collect(toList()))
        );
  }

}
