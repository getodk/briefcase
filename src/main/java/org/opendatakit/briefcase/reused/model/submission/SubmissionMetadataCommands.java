package org.opendatakit.briefcase.reused.model.submission;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.walk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.model.XmlElement;
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
        List<Path> pathStream = walk(formMetadata.getSubmissionsDir())
            .filter(file -> Files.isRegularFile(file) && file.getFileName().toString().equals("submission.xml"))
            .collect(toList());
        Stream<SubmissionMetadata> submissionMetadataStream = pathStream.stream()
            .map(submissionFile -> Pair.of(submissionFile, new SubmissionLazyMetadata(XmlElement.from(submissionFile))))
            .filter(pair -> pair.getRight().getInstanceId().isPresent())
            .map(pair -> {
              Path submissionFile = pair.getLeft();
              SubmissionLazyMetadata submissionLazyMetadata = pair.getRight();

              List<Path> attachmentFilenames = list(submissionFile.getParent())
                  .filter(file -> !file.getFileName().toString().equals("submission.xml"))
                  .map(Path::getFileName)
                  .collect(toList());

              return submissionLazyMetadata.freeze(submissionLazyMetadata.getInstanceId().orElseThrow(), submissionFile)
                  .withAttachmentFilenames(attachmentFilenames);
            });
        port.persist(submissionMetadataStream);
      });
    };
  }
}
