package org.opendatakit.briefcase.reused.model.submission;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jooq.Record1;
import org.jooq.SelectFinalStep;
import org.jooq.SelectSeekStep1;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.SubmissionMetadataRecord;

public class InMemorySubmissionMetadataAdapter implements SubmissionMetadataPort {
  @Override
  public void flush() {

  }

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
  public void persist(Stream<SubmissionMetadata> formMetadata) {

  }

  @Override
  public <T> Optional<T> fetch(SelectFinalStep<Record1<T>> where) {
    return Optional.empty();
  }

  @Override
  public Stream<SubmissionMetadata> fetchAll(SelectSeekStep1<SubmissionMetadataRecord, OffsetDateTime> where) {
    return null;
  }
}
