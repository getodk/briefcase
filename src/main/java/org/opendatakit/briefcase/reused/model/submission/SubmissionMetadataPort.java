package org.opendatakit.briefcase.reused.model.submission;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public interface SubmissionMetadataPort {

  void flush();

  <T> T query(Function<SubmissionMetadataPort, T> query);

  void execute(Consumer<SubmissionMetadataPort> command);

  void persist(SubmissionMetadata submissionMetadata);

  void persist(Stream<SubmissionMetadata> submissionMetadataStream);

  boolean hasBeenAlreadyPulled(String formId, String instanceId);

  Stream<SubmissionMetadata> sortedSubmissions(FormKey formKey);

  Stream<SubmissionMetadata> sortedSubmissions(FormMetadata formMetadata, DateRange dateRange, boolean smartAppend);
}
