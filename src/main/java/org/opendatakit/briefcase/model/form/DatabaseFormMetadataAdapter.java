package org.opendatakit.briefcase.model.form;

import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.truncate;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.jooq.Tables.FORM_METADATA;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.opendatakit.briefcase.jooq.Sequences;
import org.opendatakit.briefcase.jooq.tables.records.FormMetadataRecord;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class DatabaseFormMetadataAdapter implements FormMetadataPort {
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  public DatabaseFormMetadataAdapter(Supplier<DSLContext> dslContextSupplier) {
    this.dslContextSupplier = dslContextSupplier;
  }

  private DSLContext getDslContext() {
    return dslContextCache.computeIfAbsent("default", __ -> dslContextSupplier.get());
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    command.accept(this);
  }

  @Override
  public void flush() {
    getDslContext().execute(truncate(FORM_METADATA));
  }

  @Override
  public void persist(FormMetadata formMetadata) {
    // TODO Use generated records and let jOOQ do the work instead of explicitly using all the fields in the table, because this will break each time we change the table structure
    getDslContext().execute(mergeInto(FORM_METADATA)
        .using(selectOne())
        .on(Stream.of(
            FORM_METADATA.FORM_NAME.eq(formMetadata.getKey().getName()),
            FORM_METADATA.FORM_ID.eq(formMetadata.getKey().getId()),
            FORM_METADATA.FORM_VERSION.eq(formMetadata.getKey().getVersion().orElse(null))
        ).reduce(trueCondition(), Condition::and))
        .whenMatchedThenUpdate()
        .set(FORM_METADATA.FORM_DIR, formMetadata.getFormDir().toString())
        .set(FORM_METADATA.FORM_FILENAME, formMetadata.getFormFilename().toString())
        .set(FORM_METADATA.CURSOR_TYPE, formMetadata.getCursor().getType().getName())
        .set(FORM_METADATA.CURSOR_VALUE, formMetadata.getCursor().getValue())
        .set(FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE, formMetadata.getLastExportedSubmissionDate().orElse(null))
        .whenNotMatchedThenInsert(
            FORM_METADATA.ID,
            FORM_METADATA.FORM_NAME,
            FORM_METADATA.FORM_ID,
            FORM_METADATA.FORM_VERSION,
            FORM_METADATA.FORM_DIR,
            FORM_METADATA.FORM_FILENAME,
            FORM_METADATA.CURSOR_TYPE,
            FORM_METADATA.CURSOR_VALUE,
            FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE
        )
        .values(
            Sequences.FORM_METADATA_ID_SEQ.nextval(),
            value(formMetadata.getKey().getName()),
            value(formMetadata.getKey().getId()),
            value(formMetadata.getKey().getVersion().orElse(null)),
            value(formMetadata.getFormDir().toString()),
            value(formMetadata.getFormFilename().toString()),
            value(formMetadata.getCursor().getType().getName()),
            value(formMetadata.getCursor().getValue()),
            value(formMetadata.getLastExportedSubmissionDate().orElse(null))
        )
    );
  }

  @Override
  public void persist(Stream<FormMetadata> formMetadata) {
    // TODO Somehow migrate this into a bulk upsert SQL
    formMetadata.forEach(this::persist);
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return getDslContext().fetchOptional(selectFrom(FORM_METADATA)
        .where(Stream.of(
            FORM_METADATA.FORM_NAME.eq(key.getName()),
            FORM_METADATA.FORM_ID.eq(key.getId()),
            FORM_METADATA.FORM_VERSION.eq(key.getVersion().orElse(null))
        ).reduce(trueCondition(), Condition::and))
    ).map(DatabaseFormMetadataAdapter::mapToDomain);
  }

  @Override
  public Stream<FormMetadata> fetchAll() {
    return getDslContext().fetchStream(FORM_METADATA).map(DatabaseFormMetadataAdapter::mapToDomain);
  }

  private static FormMetadata mapToDomain(FormMetadataRecord record) {
    return new FormMetadata(
        FormKey.of(
            record.getFormName(),
            record.getFormId(),
            Optional.ofNullable(record.getFormVersion())
        ),
        Paths.get(record.getFormDir()),
        Paths.get(record.getFormFilename()),
        true,
        Cursor.Type.from(record.getCursorType()).create(record.getCursorValue()),
        Optional.ofNullable(record.getLastExportedSubmissionDate())
    );
  }
}
