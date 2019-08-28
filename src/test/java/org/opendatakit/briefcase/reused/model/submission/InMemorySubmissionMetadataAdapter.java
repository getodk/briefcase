package org.opendatakit.briefcase.reused.model.submission;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class InMemorySubmissionMetadataAdapter implements SubmissionMetadataPort {
  private final Map<SubmissionKey, SubmissionMetadata> store = new HashMap<>();

  @Override
  public void flush() {
    store.clear();
  }

  @Override
  public <T> T query(Function<SubmissionMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<SubmissionMetadataPort> command) {
    command.accept(this);
  }

  @Override
  public void persist(SubmissionMetadata submissionMetadata) {
    store.put(submissionMetadata.getKey(), submissionMetadata);
  }

  @Override
  public void persist(Stream<SubmissionMetadata> submissionMetadataStream) {
    submissionMetadataStream.forEach(this::persist);
  }

  @Override
  public boolean hasBeenAlreadyPulled(String formId, String instanceId) {
    return store.keySet().stream()
        .anyMatch(key -> key.getFormId().equalsIgnoreCase(formId) && key.getInstanceId().equals(instanceId));
  }

  @Override
  public Stream<SubmissionMetadata> sortedSubmissions(FormKey formKey) {
    return store.values().stream()
        .filter(submissionMetadata1 -> isSameForm(submissionMetadata1, formKey))
        .sorted(Comparator.comparing(this::coalescedSubmissionDateTime));
  }

  @Override
  public Stream<SubmissionMetadata> sortedSubmissions(FormMetadata formMetadata, DateRange dateRange, boolean smartAppend) {
    return store.values().stream()
        .filter(submissionMetadata1 -> isSameForm(submissionMetadata1, formMetadata.getKey()))
        .filter(submissionMetadata1 -> dateRange.contains(coalescedSubmissionDateTime(submissionMetadata1)))
        .filter(submissionMetadata -> true
            || !smartAppend
            || formMetadata.getLastExportedSubmissionDate().isEmpty()
            || coalescedSubmissionDateTime(submissionMetadata).isAfter(formMetadata.getLastExportedSubmissionDate().get()))
        .sorted(Comparator.comparing(this::coalescedSubmissionDateTime));
  }

  private boolean isSameForm(SubmissionMetadata submissionMetadata, FormKey formKey) {
    return submissionMetadata.getKey().getFormId().equals(formKey.getId());
  }

  private OffsetDateTime coalescedSubmissionDateTime(SubmissionMetadata submissionMetadata) {
    return submissionMetadata.getSubmissionDateTime().orElse(SubmissionMetadata.MIN_SUBMISSION_DATE_TIME);
  }

}
