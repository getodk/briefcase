package org.opendatakit.briefcase.reused.model.submission;

import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.using;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectFinalStep;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.SubmissionMetadataRecord;

public class DatabaseSubmissionMetadataAdapter implements SubmissionMetadataPort {
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  public DatabaseSubmissionMetadataAdapter(Supplier<DSLContext> dslContextSupplier) {
    this.dslContextSupplier = dslContextSupplier;
  }

  private DSLContext getDslContext() {
    return dslContextCache.computeIfAbsent("default", __ -> dslContextSupplier.get());
  }

  @Override
  public <T> T query(Function<SubmissionMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<SubmissionMetadataPort> command) {
    getDslContext().transaction(conf ->
        command.accept(new DatabaseSubmissionMetadataAdapter(() -> using(conf)))
    );
  }

  @Override
  public void persist(SubmissionMetadata submissionMetadata) {
    getDslContext().executeInsert(mapToDomain(submissionMetadata));
  }

  @Override
  public <T> Optional<T> fetch(SelectFinalStep<Record1<T>> query) {
    return getDslContext().fetchOptional(query).map(Record1::component1);
  }

  @Override
  public <T> Stream<T> fetchAll(SelectFinalStep<Record1<T>> query) {
    return getDslContext().fetchStream(query).map(Record1::component1);
  }

  private static SubmissionMetadataRecord mapToDomain(SubmissionMetadata submissionMetadata) {
    return new SubmissionMetadataRecord(
        submissionMetadata.getKey().getFormId(),
        submissionMetadata.getKey().getFormVersion().orElse(""),
        submissionMetadata.getKey().getInstanceId(),
        submissionMetadata.getSubmissionFile().map(Objects::toString).orElseThrow(BriefcaseException::new),
        submissionMetadata.getSubmissionDateTime().orElse(null),
        submissionMetadata.getEncryptedXmlFilename().map(Objects::toString).orElse(null),
        submissionMetadata.getBase64EncryptedKey().orElse(null),
        submissionMetadata.getEncryptedSignature().orElse(null),
        submissionMetadata.getAttachmentFilenames().stream().map(Objects::toString).collect(toList()).toArray(new String[submissionMetadata.getAttachmentFilenames().size()])
    );
  }
}
