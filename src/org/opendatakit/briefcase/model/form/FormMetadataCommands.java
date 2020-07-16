package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Cursor cursor, Path storageRoot, Path formDir) {
    return port -> {
      Optional<FormMetadata> fetch = port
          .fetch(key);
      FormMetadata formMetadata = fetch
          .orElseGet(() -> FormMetadata.of(key, storageRoot, formDir));
      FormMetadata formMetadata1 = formMetadata
          .withHasBeenPulled(true, new HashSet<>())
          .withCursor(cursor);
      port.persist(formMetadata1);
    };
  }

  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Path storageRoot, Path formDir) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, storageRoot, formDir))
        .withHasBeenPulled(true, new HashSet<>()));
  }

  public static Consumer<FormMetadataPort> updateLastExportedSubmission(FormKey key, String instanceId, OffsetDateTime submissionDate, OffsetDateTime exportDateTime, Path storageRoot, Path formDir) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, storageRoot, formDir))
        .withLastExportedSubmission(instanceId, submissionDate, exportDateTime));
  }

  public static Consumer<FormMetadataPort> cleanAllCursors() {
    return port -> port.persist(port.fetchAll().map(FormMetadata::withoutCursor));
  }
}
