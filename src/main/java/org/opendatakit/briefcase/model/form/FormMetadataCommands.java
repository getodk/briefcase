package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Cursor cursor, Path formDir, Path formFilename) {
    return port -> {
      Optional<FormMetadata> fetch = port
          .fetch(key);
      FormMetadata formMetadata = fetch
          .orElseGet(() -> FormMetadata.of(key, formDir, formFilename));
      FormMetadata formMetadata1 = formMetadata
          .withHasBeenPulled(true)
          .withCursor(cursor);
      port.persist(formMetadata1);
    };
  }

  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Path formDir, Path formfilename) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, formDir, formfilename))
        .withHasBeenPulled(true));
  }

  public static Consumer<FormMetadataPort> updateLastExportedSubmission(FormKey key, String instanceId, OffsetDateTime submissionDate, OffsetDateTime exportDateTime, Path formDir, Path formFilename) {
    return port -> port.persist(port
        .fetch(key)
        .orElseGet(() -> FormMetadata.of(key, formDir, formFilename))
        .withLastExportedSubmissionDate(submissionDate));
  }

  public static Consumer<FormMetadataPort> cleanAllCursors() {
    return port -> port.persist(port.fetchAll().map(FormMetadata::withoutCursor));
  }
}
