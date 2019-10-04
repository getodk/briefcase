package org.opendatakit.briefcase.reused.model.form;

import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.truncate;
import static org.jooq.impl.DSL.update;
import static org.jooq.impl.DSL.using;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.FORM_METADATA;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
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
import org.jooq.Record1;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.api.Json;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.FormMetadataRecord;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

public class DatabaseFormMetadataAdapter implements FormMetadataPort {
  private final Workspace workspace;
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  private DatabaseFormMetadataAdapter(Workspace workspace, Supplier<DSLContext> dslContextSupplier) {
    this.workspace = workspace;
    this.dslContextSupplier = dslContextSupplier;
  }

  public static FormMetadataPort from(Workspace workspace, BriefcaseDb db) {
    return new DatabaseFormMetadataAdapter(workspace, db::getDslContext);
  }

  private DSLContext getDslContext() {
    return dslContextCache.computeIfAbsent("default", __ -> dslContextSupplier.get());
  }

  @Override
  public void flush() {
    getDslContext().execute(truncate(FORM_METADATA));
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    getDslContext().transaction(conf ->
        command.accept(new DatabaseFormMetadataAdapter(workspace, () -> using(conf)))
    );
  }

  @Override
  public void persist(FormMetadata formMetadata) {
    // TODO Use generated records and let jOOQ do the work instead of explicitly using all the fields in the table, because this will break each time we change the table structure
    ObjectMapper mapper = Json.getMapper();
    getDslContext().execute(mergeInto(FORM_METADATA)
        .using(selectOne())
        .on(getMatchingCriteria(formMetadata.getKey()))
        .whenMatchedThenUpdate()
        .set(FORM_METADATA.FORM_NAME, formMetadata.getFormName().orElse(null))
        .set(FORM_METADATA.FORM_FILE, workspace.relativize(formMetadata.getFormFile()).toString())
        .set(FORM_METADATA.CURSOR_TYPE, formMetadata.getCursor().getType().getName())
        .set(FORM_METADATA.CURSOR_VALUE, formMetadata.getCursor().getValue())
        .set(FORM_METADATA.IS_ENCRYPTED, formMetadata.isEncrypted())
        .set(FORM_METADATA.URL_MANIFEST, formMetadata.getManifestUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.URL_DOWNLOAD, formMetadata.getDownloadUrl().map(Objects::toString).orElse(null))
        .set(FORM_METADATA.LAST_EXPORTED_DATE_TIME, formMetadata.getLastExportedDateTime().orElse(null))
        .set(FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE_TIME, formMetadata.getLastExportedSubmissionDateTime().orElse(null))
        .set(FORM_METADATA.PULL_SOURCE, formMetadata.getPullSource().map(sot -> sot.asJson(mapper)).map(Json::serialize).orElse(null))
        .set(FORM_METADATA.EXPORT_CONFIGURATION, formMetadata.getExportConfiguration().map(ec -> ec.asJson(mapper)).map(Json::serialize).orElse(null))
        // TODO deal with the new fields pull_source_type and pull_source_value
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
            FORM_METADATA.LAST_EXPORTED_DATE_TIME,
            FORM_METADATA.LAST_EXPORTED_SUBMISSION_DATE_TIME,
            FORM_METADATA.PULL_SOURCE,
            FORM_METADATA.EXPORT_CONFIGURATION
        )
        .values(
            value(formMetadata.getKey().getId()),
            value(formMetadata.getKey().getVersion().orElse("")),
            value(formMetadata.getFormName().orElse(null)),
            value(workspace.relativize(formMetadata.getFormFile()).toString()),
            value(formMetadata.getCursor().getType().getName()),
            value(formMetadata.getCursor().getValue()),
            value(formMetadata.isEncrypted()),
            value(formMetadata.getManifestUrl().map(Objects::toString).orElse(null)),
            value(formMetadata.getDownloadUrl().map(Objects::toString).orElse(null)),
            value(formMetadata.getLastExportedDateTime().orElse(null)),
            value(formMetadata.getLastExportedSubmissionDateTime().orElse(null)),
            value(formMetadata.getPullSource().map(sot -> sot.asJson(mapper)).map(Json::serialize).orElse(null)),
            value(formMetadata.getExportConfiguration().map(ec -> ec.asJson(mapper)).map(Json::serialize).orElse(null))
            // TODO deal with the new fields pull_source_type and pull_source_value
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
        .map(this::mapToDomain);
  }

  @Override
  public Stream<FormMetadata> fetchAll() {
    return getDslContext().fetchStream(FORM_METADATA).map(this::mapToDomain);
  }

  @Override
  public void forgetPullSources() {
    getDslContext().execute(update(FORM_METADATA)
        .set(FORM_METADATA.PULL_SOURCE, (String) null));
  }

  @Override
  public ExportConfiguration getExportConfiguration(FormKey formKey) {
    return getDslContext().fetchOptional(select(FORM_METADATA.EXPORT_CONFIGURATION)
        .from(FORM_METADATA)
        .where(getMatchingCriteria(formKey)))
        .map(Record1::component1)
        .map(Json::deserialize)
        .map(ExportConfiguration::from)
        .orElse(empty().build());
  }

  @Override
  public Optional<OffsetDateTime> getLastExportDateTime(FormKey formKey) {
    return getDslContext().fetchOptional(select(FORM_METADATA.LAST_EXPORTED_DATE_TIME)
        .from(FORM_METADATA)
        .where(getMatchingCriteria(formKey)))
        .map(Record1::component1);
  }

  @Override
  public Optional<FormMetadata> fetchWithFormIdWithoutPullSource(String formId) {
    return getDslContext().fetchOptional(selectFrom(FORM_METADATA)
        .where(FORM_METADATA.FORM_ID.eq(formId).and(FORM_METADATA.PULL_SOURCE.isNull())))
        .map(this::mapToDomain);
  }

  private FormMetadata mapToDomain(FormMetadataRecord record) {
    return new FormMetadata(
        FormKey.of(
            record.getFormId(),
            Optional.ofNullable(record.getFormVersion())
        ),
        Optional.ofNullable(record.getFormName()),
        Optional.ofNullable(record.getFormFile()).map(Paths::get).map(workspace::resolve),
        Cursor.Type.from(record.getCursorType()).create(record.getCursorValue()),
        record.getIsEncrypted(),
        Optional.ofNullable(record.getUrlManifest()).map(RequestBuilder::url),
        Optional.ofNullable(record.getUrlDownload()).map(RequestBuilder::url),
        Optional.ofNullable(record.getLastExportedDateTime()),
        Optional.ofNullable(record.getLastExportedSubmissionDateTime()),
        Optional.ofNullable(record.getPullSource()).map(Json::deserialize).map(SourceOrTarget::from),
        Optional.ofNullable(record.getExportConfiguration()).map(Json::deserialize).map(ExportConfiguration::from)
    );
  }

  private static Condition getMatchingCriteria(FormKey key) {
    return trueCondition()
        .and(FORM_METADATA.FORM_ID.eq(key.getId()))
        .and(FORM_METADATA.FORM_VERSION.eq(key.getVersion().orElse("")));
  }
}
