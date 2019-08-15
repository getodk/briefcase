package org.opendatakit.briefcase.model.form;

import java.time.OffsetDateTime;
import java.util.function.Consumer;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Cursor cursor) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key))
        .withHasBeenPulled(true)
        .withCursor(cursor));
  }

  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key))
        .withHasBeenPulled(true));
  }

  public static Consumer<FormMetadataPort> updateLastExportedSubmission(FormKey key, String instanceId, OffsetDateTime submissionDate, OffsetDateTime exportDateTime) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key))
        .withLastExportedSubmission(instanceId, submissionDate, exportDateTime));
  }
}
