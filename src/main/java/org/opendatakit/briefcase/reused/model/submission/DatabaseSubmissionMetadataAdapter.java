package org.opendatakit.briefcase.reused.model.submission;

import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.using;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.SUBMISSION_METADATA;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.SubmissionMetadataRecord;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class DatabaseSubmissionMetadataAdapter implements SubmissionMetadataPort {
  /**
   * A date time value that's safe to be used to in queries that might
   * involve submission with null submission date times.
   */
  private static final Field<OffsetDateTime> COALESCED_SUBMISSION_DATE = DSL.coalesce(
      SUBMISSION_METADATA.SUBMISSION_DATE_TIME,
      value(SubmissionMetadata.MIN_SUBMISSION_DATE_TIME)
  );
  private final Workspace workspace;
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  private DatabaseSubmissionMetadataAdapter(Workspace workspace, Supplier<DSLContext> dslContextSupplier) {
    this.workspace = workspace;
    this.dslContextSupplier = dslContextSupplier;
  }

  public static SubmissionMetadataPort from(Workspace workspace, BriefcaseDb db) {
    return new DatabaseSubmissionMetadataAdapter(workspace, db::getDslContext);
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
        command.accept(new DatabaseSubmissionMetadataAdapter(workspace, () -> using(conf)))
    );
  }

  @Override
  public void persist(SubmissionMetadata submissionMetadata) {
    getDslContext().executeInsert(mapToDomain(submissionMetadata));
  }

  @Override
  public void persist(Stream<SubmissionMetadata> submissionMetadataStream) {
    getDslContext().batchInsert(submissionMetadataStream.map(this::mapToDomain).collect(toList())).execute();
  }

  @Override
  public boolean hasBeenAlreadyPulled(String formId, String instanceId) {
    return getDslContext().fetchOptional(select(count(SUBMISSION_METADATA.INSTANCE_ID))
        .from(SUBMISSION_METADATA)
        .where(trueCondition()
            .and(SUBMISSION_METADATA.FORM_ID.eq(formId))
            .and(SUBMISSION_METADATA.INSTANCE_ID.eq(instanceId)))).map(Record1::component1)
        .map(count -> count >= 1)
        .orElse(false);
  }

  @Override
  public Stream<SubmissionMetadata> sortedSubmissions(FormKey formKey) {
    return sortedSubmissions(asList(
        SUBMISSION_METADATA.FORM_ID.eq(formKey.getId()),
        SUBMISSION_METADATA.FORM_VERSION.eq(formKey.getVersion().orElse(""))
    ));
  }

  @Override
  public Stream<SubmissionMetadata> sortedSubmissions(FormMetadata formMetadata, DateRange dateRange, boolean smartAppend) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(SUBMISSION_METADATA.FORM_ID.eq(formMetadata.getKey().getId()));
    conditions.add(SUBMISSION_METADATA.FORM_VERSION.eq(formMetadata.getKey().getVersion().orElse("")));
    if (!dateRange.isEmpty())
      conditions.add(COALESCED_SUBMISSION_DATE.between(
          dateRange.getStart().atStartOfDay().atZone(systemDefault()).toOffsetDateTime(),
          dateRange.getEnd().atStartOfDay().atZone(systemDefault()).toOffsetDateTime()
      ));

    if (smartAppend && formMetadata.getLastExportedSubmissionDateTime().isPresent())
      conditions.add(COALESCED_SUBMISSION_DATE.greaterThan(formMetadata.getLastExportedSubmissionDateTime().get()));

    return sortedSubmissions(conditions);
  }

  private Stream<SubmissionMetadata> sortedSubmissions(List<Condition> conditions) {
    return getDslContext().fetchStream(selectFrom(SUBMISSION_METADATA)
        .where(conditions.stream().reduce(trueCondition(), Condition::and))
        .orderBy(COALESCED_SUBMISSION_DATE.asc()))
        .map(record -> new SubmissionMetadata(
            new SubmissionKey(
                record.getFormId(),
                Optional.ofNullable(record.getFormVersion()).filter(not(String::isBlank)),
                record.getInstanceId()
            ),
            Optional.ofNullable(record.getSubmissionFile()).map(Paths::get).map(workspace::resolve),
            Optional.ofNullable(record.getSubmissionDateTime()),
            Optional.ofNullable(record.getEncryptedXmlFilename()).map(Paths::get),
            Optional.ofNullable(record.getBase_64EncryptedKey()),
            Optional.ofNullable(record.getEncryptedSignature()),
            getStringArray(record, SUBMISSION_METADATA.ATTACHMENT_FILENAMES).map(Paths::get).collect(Collectors.toList())
        ));
  }

  private Stream<String> getStringArray(Record record, Field<?> field) {
    try {
      return Arrays.stream((Object[]) ((JDBCArray) record.get(field)).getArray()).map(o -> (String) o);
    } catch (SQLException e) {
      throw new BriefcaseException(e);
    }
  }

  private SubmissionMetadataRecord mapToDomain(SubmissionMetadata submissionMetadata) {
    return new SubmissionMetadataRecord(
        submissionMetadata.getKey().getFormId(),
        submissionMetadata.getKey().getFormVersion().orElse(""),
        submissionMetadata.getKey().getInstanceId(),
        workspace.relativize(submissionMetadata.getSubmissionFile()).toString(),
        submissionMetadata.getSubmissionDateTime().orElse(null),
        submissionMetadata.getEncryptedXmlFilename().map(Objects::toString).orElse(null),
        submissionMetadata.getBase64EncryptedKey().orElse(null),
        submissionMetadata.getEncryptedSignature().orElse(null),
        submissionMetadata.getAttachmentFilenames().stream().map(Objects::toString).collect(toList()).toArray(new String[submissionMetadata.getAttachmentFilenames().size()])
    );
  }
}
