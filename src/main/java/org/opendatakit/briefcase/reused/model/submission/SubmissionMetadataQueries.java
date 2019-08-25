package org.opendatakit.briefcase.reused.model.submission;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.trueCondition;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.SUBMISSION_METADATA;

import java.util.function.Function;

public class SubmissionMetadataQueries {
  public static Function<SubmissionMetadataPort, Boolean> hasBeenAlreadyPulled(String formId, String instanceId) {
    return port -> port.fetch(select(count(SUBMISSION_METADATA.INSTANCE_ID))
        .from(SUBMISSION_METADATA)
        .where(trueCondition()
            .and(SUBMISSION_METADATA.FORM_ID.eq(formId))
            .and(SUBMISSION_METADATA.INSTANCE_ID.eq(instanceId))))
        .map(count -> count >= 1)
        .orElse(false);
  }
}
