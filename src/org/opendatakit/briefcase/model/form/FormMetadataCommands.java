package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Cursor cursor, Path storageDirectory) {
    return port -> {
      Optional<FormMetadata> fetch = port
          .fetch(key);
      FormMetadata formMetadata = fetch
          .orElseGet(() -> FormMetadata.of(key, storageDirectory));
      FormMetadata formMetadata1 = formMetadata
          .withHasBeenPulled(true)
          .withCursor(cursor);
      port.persist(formMetadata1);
    };
  }

  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Path storageDirectory) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, storageDirectory))
        .withHasBeenPulled(true));
  }

  public static Consumer<FormMetadataPort> updateLastExportedSubmission(FormKey key, String instanceId, OffsetDateTime submissionDate, OffsetDateTime exportDateTime, Path storageDirectory) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, storageDirectory))
        .withLastExportedSubmission(instanceId, submissionDate, exportDateTime));
  }
}
