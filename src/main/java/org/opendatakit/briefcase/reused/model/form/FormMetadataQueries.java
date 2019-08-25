package org.opendatakit.briefcase.reused.model.form;

import java.util.Optional;
import java.util.function.Function;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;

public class FormMetadataQueries {
  public static Function<FormMetadataPort, Optional<Cursor>> lastCursorOf(FormKey key) {
    return port -> port.fetch(key).map(FormMetadata::getCursor);
  }
}
