package org.opendatakit.briefcase.reused.model.submission;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jooq.Record1;
import org.jooq.SelectFinalStep;

public class InMemorySubmissionMetadataAdapter implements SubmissionMetadataPort {
  private final Map<SubmissionKey, Object> store = new HashMap<>();

  @Override
  public <T> T query(Function<SubmissionMetadataPort, T> query) {
    return null;
  }

  @Override
  public void execute(Consumer<SubmissionMetadataPort> command) {

  }

  @Override
  public void persist(SubmissionMetadata formMetadata) {

  }

  @Override
  public <T> Optional<T> fetch(SelectFinalStep<Record1<T>> where) {
    return Optional.empty();
  }

  @Override
  public <T> Stream<T> fetchAll(SelectFinalStep<Record1<T>> where) {
    return Stream.empty();
  }
}
