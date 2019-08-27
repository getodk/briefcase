package org.opendatakit.briefcase.reused.model.submission;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.using;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.SUBMISSION_METADATA;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hsqldb.jdbc.JDBCArray;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectFinalStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
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
  public void flush() {
    getDslContext().execute(DSL.truncate(SUBMISSION_METADATA));
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
  public void persist(Stream<SubmissionMetadata> submissionMetadata) {
    getDslContext().batchInsert(submissionMetadata.map(DatabaseSubmissionMetadataAdapter::mapToDomain).collect(toList())).execute();
  }


  @Override
  public <T> Optional<T> fetch(SelectFinalStep<Record1<T>> query) {
    return getDslContext().fetchOptional(query).map(Record1::component1);
  }

  @Override
  public Stream<SubmissionMetadata> fetchAll(SelectSeekStep1<SubmissionMetadataRecord, OffsetDateTime> query) {
    return getDslContext().fetchStream(query).map(record -> new SubmissionMetadata(
        new SubmissionKey(
            record.getFormId(),
            Optional.ofNullable(record.getFormVersion()).filter(not(String::isBlank)),
            record.getInstanceId()
        ),
        Optional.ofNullable(record.getSubmissionFilename()).map(Paths::get),
        Optional.ofNullable(record.getSubmissionDateTime()),
        Optional.ofNullable(record.getEncryptedXmlFilename()).map(Paths::get),
        Optional.ofNullable(record.getBase_64EncryptedKey()),
        Optional.ofNullable(record.getEncryptedSignature()),
        getStringArray(record, SUBMISSION_METADATA.ATTACHMENT_FILENAMES).map(Paths::get).collect(Collectors.toList())
    ));
  }

  @SuppressWarnings("unchecked")
  private Stream<String> getStringArray(Record record, Field<?> field) {
    try {
      return Arrays.stream((Object[]) ((JDBCArray) record.get(field)).getArray()).map(o -> (String) o);
    } catch (SQLException e) {
      throw new BriefcaseException(e);
    }
  }

  private static SubmissionMetadataRecord mapToDomain(SubmissionMetadata submissionMetadata) {
    return new SubmissionMetadataRecord(
        submissionMetadata.getKey().getFormId(),
        submissionMetadata.getKey().getFormVersion().orElse(""),
        submissionMetadata.getKey().getInstanceId(),
        submissionMetadata.getSubmissionFile().toString(),
        submissionMetadata.getSubmissionDateTime().orElse(null),
        submissionMetadata.getEncryptedXmlFilename().map(Objects::toString).orElse(null),
        submissionMetadata.getBase64EncryptedKey().orElse(null),
        submissionMetadata.getEncryptedSignature().orElse(null),
        submissionMetadata.getAttachmentFilenames().stream().map(Objects::toString).collect(toList()).toArray(new String[submissionMetadata.getAttachmentFilenames().size()])
    );
  }
}
