package org.opendatakit.briefcase.model.form;

import java.util.Optional;
import java.util.function.Function;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataQueries {
  public static Function<FormMetadataPort, Optional<Cursor>> lastCursorOf(FormKey key) {
    return port -> Optional.empty();
  }
}
