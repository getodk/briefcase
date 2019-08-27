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

public interface SubmissionMetadataPort {

  void flush();

  <T> T query(Function<SubmissionMetadataPort, T> query);

  void execute(Consumer<SubmissionMetadataPort> command);

  void persist(SubmissionMetadata formMetadata);

  void persist(Stream<SubmissionMetadata> formMetadata);

  // These break the clean architecture. This interface shouldn't have jOOQ dependencies

  <T> Optional<T> fetch(SelectFinalStep<Record1<T>> where);

  Stream<SubmissionMetadata> fetchAll(SelectSeekStep1<SubmissionMetadataRecord, OffsetDateTime> where);
}
