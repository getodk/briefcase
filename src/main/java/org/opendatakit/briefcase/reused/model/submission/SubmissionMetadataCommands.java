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

  public static Consumer<SubmissionMetadataPort> syncSubmissions(Stream<FormMetadata> formMetadataStream) {
    return port -> {
      port.flush();
      formMetadataStream.forEach(formMetadata -> {
        if (!Files.exists(formMetadata.getSubmissionsDir()))
          return;

        Stream<SubmissionMetadata> submissionMetadataStream = walk(formMetadata.getSubmissionsDir())
            .filter(SubmissionMetadata::isSubmissionFile)
            .filter(SubmissionMetadata::hasInstanceId)
            .map(submissionFile -> SubmissionMetadata.from(
                submissionFile,
                list(submissionFile.getParent())
                    .filter(not(SubmissionMetadata::isSubmissionFile))
                    .map(Path::getFileName)
                    .collect(toList()))
            );

        port.persist(submissionMetadataStream);
      });
    };
  }

}
