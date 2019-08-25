package org.opendatakit.briefcase.reused.model.submission;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;

public class SubmissionMetadataCommands {
  public static Consumer<SubmissionMetadataPort> insert(SubmissionMetadata submissionMetadata) {
    return port -> port.persist(submissionMetadata);
  }

  public static Consumer<FormMetadataPort> sync(Stream<FormMetadata> formMetadataStream) {
    return port -> {};
  }

}
