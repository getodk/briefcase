package org.opendatakit.briefcase.reused.model.submission;

import static java.time.ZoneOffset.systemDefault;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.SUBMISSION_METADATA;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.SubmissionMetadataRecord;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class SubmissionMetadataQueries {

  private static final Field<OffsetDateTime> COALESCED_SUBMISSION_DATE = DSL.coalesce(
      SUBMISSION_METADATA.SUBMISSION_DATE_TIME,
      value(OffsetDateTime.parse("1970-01-01T00:00:00.000Z"))
  );

  public static Function<SubmissionMetadataPort, Boolean> hasBeenAlreadyPulled(String formId, String instanceId) {
    return port -> port.fetch(select(count(SUBMISSION_METADATA.INSTANCE_ID))
        .from(SUBMISSION_METADATA)
        .where(trueCondition()
            .and(SUBMISSION_METADATA.FORM_ID.eq(formId))
            .and(SUBMISSION_METADATA.INSTANCE_ID.eq(instanceId))))
        .map(count -> count >= 1)
        .orElse(false);
  }

  public static Function<SubmissionMetadataPort, Stream<SubmissionMetadata>> sortedSubmissions(FormKey formKey) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(SUBMISSION_METADATA.FORM_ID.eq(formKey.getId()));
    conditions.add(SUBMISSION_METADATA.FORM_VERSION.eq(formKey.getVersion().orElse("")));

    SelectSeekStep1<SubmissionMetadataRecord, OffsetDateTime> query = selectFrom(SUBMISSION_METADATA)
        .where(conditions.stream().reduce(trueCondition(), Condition::and))
        .orderBy(COALESCED_SUBMISSION_DATE.asc());

    return port -> port.fetchAll(query);
  }

  public static Function<SubmissionMetadataPort, Stream<SubmissionMetadata>> sortedSubmissions(FormMetadata formMetadata, DateRange dateRange, boolean smartAppend) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(SUBMISSION_METADATA.FORM_ID.eq(formMetadata.getKey().getId()));
    conditions.add(SUBMISSION_METADATA.FORM_VERSION.eq(formMetadata.getKey().getVersion().orElse("")));
    if (!dateRange.isEmpty())
      conditions.add(COALESCED_SUBMISSION_DATE.between(
          dateRange.getStart().atStartOfDay().atZone(systemDefault()).toOffsetDateTime(),
          dateRange.getEnd().atStartOfDay().atZone(systemDefault()).toOffsetDateTime()
      ));

    if (smartAppend && formMetadata.getLastExportedSubmissionDate().isPresent())
      conditions.add(COALESCED_SUBMISSION_DATE.greaterThan(formMetadata.getLastExportedSubmissionDate().get()));

    SelectSeekStep1<SubmissionMetadataRecord, OffsetDateTime> query = selectFrom(SUBMISSION_METADATA)
        .where(conditions.stream().reduce(trueCondition(), Condition::and))
        .orderBy(COALESCED_SUBMISSION_DATE.asc());

    return port -> port.fetchAll(query);
  }
}
