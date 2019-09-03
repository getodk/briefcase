package org.opendatakit.briefcase.reused.model.form;

import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.using;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.FORM_METADATA;

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
import org.jooq.impl.DSL;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.FormMetadataRecord;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

public class DatabaseFormMetadataAdapter implements FormMetadataPort {
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  private DatabaseFormMetadataAdapter(Supplier<DSLContext> dslContextSupplier) {
    this.dslContextSupplier = dslContextSupplier;
  }

  public static FormMetadataPort from(BriefcaseDb db) {
    return new DatabaseFormMetadataAdapter(db::getDslContext);
  }

  private DSLContext getDslContext() {
    return dslContextCache.computeIfAbsent("default", __ -> dslContextSupplier.get());
  }

  @Override
  public void flush() {
    getDslContext().execute(DSL.truncate(FORM_METADATA));
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
  public void persist(FormMetadata formMetadata) {
    // TODO Use generated records and let jOOQ do the work instead of explicitly using all the fields in the table, because this will break each time we change the table structure
    getDslContext().execute(mergeInto(FORM_METADATA)
        .using(selectOne())
        .on(getMatchingCriteria(formMetadata.getKey()))
        .whenMatchedThenUpdate()
        .set(FORM_METADATA.FORM_NAME, formMetadata.getFormName().orElse(null))
        .set(FORM_METADATA.FORM_FILE, formMetadata.getFormFile().toString())
        .set(FORM_METADATA.CURSOR_TYPE, formMetadata.getCursor().getType().getName())
        .set(FORM_METADATA.CURSOR_VALUE, formMetadata.getCursor().getValue())
        .set(FORM_METADATA.IS_ENCRYPTED, formMetadata.isEncrypted())
        .set(FORM_METADATA.URL_MANIFEST, formMetadata.getManifestUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.URL_DOWNLOAD, formMetadata.getDownloadUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE, formMetadata.getLastExportedSubmissionDate().orElse(null))
        .whenNotMatchedThenInsert(
            FORM_METADATA.FORM_ID,
            FORM_METADATA.FORM_VERSION,
            FORM_METADATA.FORM_NAME,
            FORM_METADATA.FORM_FILE,
            FORM_METADATA.CURSOR_TYPE,
            FORM_METADATA.CURSOR_VALUE,
            FORM_METADATA.IS_ENCRYPTED,
            FORM_METADATA.URL_MANIFEST,
            FORM_METADATA.URL_DOWNLOAD,
            FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE
        )
        .values(
            value(formMetadata.getKey().getId()),
            value(formMetadata.getKey().getVersion().orElse("")),
            value(formMetadata.getFormName().orElse(null)),
            value(formMetadata.getFormFile().toString()),
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
    return getDslContext()
        .fetchOptional(selectFrom(FORM_METADATA).where(getMatchingCriteria(key)))
        .map(DatabaseFormMetadataAdapter::mapToDomain);
  }

  @Override
  public Stream<FormMetadata> fetchAll() {
    return getDslContext().fetchStream(FORM_METADATA).map(DatabaseFormMetadataAdapter::mapToDomain);
  }

  private static FormMetadata mapToDomain(FormMetadataRecord record) {
    return new FormMetadata(
        FormKey.of(
            record.getFormId(),
            Optional.ofNullable(record.getFormVersion())
        ),
        Optional.ofNullable(record.getFormName()),
        Optional.ofNullable(record.getFormFile()).map(Paths::get),
        Cursor.Type.from(record.getCursorType()).create(record.getCursorValue()),
        record.getIsEncrypted(),
        Optional.ofNullable(record.getUrlManifest()).map(RequestBuilder::url),
        Optional.ofNullable(record.getUrlDownload()).map(RequestBuilder::url),
        Optional.ofNullable(record.getLastExportedSubmissionDate())
    );
  }

  private static Condition getMatchingCriteria(FormKey key) {
    return trueCondition()
        .and(FORM_METADATA.FORM_ID.eq(key.getId()))
        .and(FORM_METADATA.FORM_VERSION.eq(key.getVersion().orElse("")));
  }
}
