package org.opendatakit.briefcase.reused.model.submission;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jooq.Record1;
import org.jooq.SelectFinalStep;

public interface SubmissionMetadataPort {

  <T> T query(Function<SubmissionMetadataPort, T> query);

  void execute(Consumer<SubmissionMetadataPort> command);

  void persist(SubmissionMetadata formMetadata);

  Optional<SubmissionMetadata> fetch(SubmissionKey key);

  <T> Optional<T> fetch(SelectFinalStep<Record1<T>> where);
}
