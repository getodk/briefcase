package org.opendatakit.briefcase.model.form;

import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.truncate;
import static org.jooq.impl.DSL.using;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.jooq.Tables.FORM_METADATA;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
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
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

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
    getDslContext().transaction(conf ->
        command.accept(new DatabaseFormMetadataAdapter(() -> using(conf)))
    );
  }

  @Override
  public void flush() {
    getDslContext().execute(truncate(FORM_METADATA));
  }

  @Override
  public void persist(FormMetadata formMetadata) {
    // TODO Use generated records and let jOOQ do the work instead of explicitly using all the fields in the table, because this will break each time we change the table structure
    String version = formMetadata.getKey().getVersion().orElse(null);
    getDslContext().execute(mergeInto(FORM_METADATA)
        .using(selectOne())
        .on(Stream.of(
            FORM_METADATA.FORM_NAME.eq(formMetadata.getKey().getName()),
            FORM_METADATA.FORM_ID.eq(formMetadata.getKey().getId()),
            formMetadata.getKey().getVersion()
                .map(FORM_METADATA.FORM_VERSION::eq)
                .orElse(FORM_METADATA.FORM_VERSION.isNull())
        ).reduce(trueCondition(), Condition::and))
        .whenMatchedThenUpdate()
        .set(FORM_METADATA.FORM_FILE, formMetadata.getFormFile().map(Objects::toString).orElseThrow(BriefcaseException::new))
        .set(FORM_METADATA.CURSOR_TYPE, formMetadata.getCursor().getType().getName())
        .set(FORM_METADATA.CURSOR_VALUE, formMetadata.getCursor().getValue())
        .set(FORM_METADATA.IS_ENCRYPTED, formMetadata.isEncrypted())
        .set(FORM_METADATA.URL_MANIFEST, formMetadata.getManifestUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.URL_DOWNLOAD, formMetadata.getDownloadUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE, formMetadata.getLastExportedSubmissionDate().orElse(null))
        .whenNotMatchedThenInsert(
            FORM_METADATA.ID,
            FORM_METADATA.FORM_NAME,
            FORM_METADATA.FORM_ID,
            FORM_METADATA.FORM_VERSION,
            FORM_METADATA.FORM_FILE,
            FORM_METADATA.CURSOR_TYPE,
            FORM_METADATA.CURSOR_VALUE,
            FORM_METADATA.IS_ENCRYPTED,
            FORM_METADATA.URL_MANIFEST,
            FORM_METADATA.URL_DOWNLOAD,
            FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE
        )
        .values(
            Sequences.FORM_METADATA_ID_SEQ.nextval(),
            value(formMetadata.getKey().getName()),
            value(formMetadata.getKey().getId()),
            value(formMetadata.getKey().getVersion().orElse(null)),
            value(formMetadata.getFormFile().map(Objects::toString).orElseThrow(BriefcaseException::new)),
            value(formMetadata.getCursor().getType().getName()),
            value(formMetadata.getCursor().getValue()),
            value(formMetadata.isEncrypted()),
            value(formMetadata.getManifestUrl().map(Objects::toString).orElse(null)),
            value(formMetadata.getDownloadUrl().map(Objects::toString).orElse(null)),
            value(formMetadata.getLastExportedSubmissionDate().orElse(null))
        )
    );
  }

  @Override
  public void persist(Stream<FormMetadata> formMetadata) {
    // TODO Somehow migrate this into a bulk upsert SQL
    execute(port -> formMetadata.forEach(port::persist));
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return getDslContext().fetchOptional(selectFrom(FORM_METADATA)
        .where(Stream.of(
            FORM_METADATA.FORM_NAME.eq(key.getName()),
            FORM_METADATA.FORM_ID.eq(key.getId()),
            key.getVersion()
                .map(FORM_METADATA.FORM_VERSION::eq)
                .orElse(FORM_METADATA.FORM_VERSION.isNull())
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
        Optional.ofNullable(record.getFormFile()).map(Paths::get),
        Cursor.Type.from(record.getCursorType()).create(record.getCursorValue()),
        record.getIsEncrypted(),
        Optional.ofNullable(record.getUrlManifest()).map(RequestBuilder::url),
        Optional.ofNullable(record.getUrlDownload()).map(RequestBuilder::url),
        Optional.ofNullable(record.getLastExportedSubmissionDate())
    );
  }
}
