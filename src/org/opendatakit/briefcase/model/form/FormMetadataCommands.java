package org.opendatakit.briefcase.model.form;

import java.util.function.Consumer;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key, Cursor cursor) {
    return port -> {};
  }

  public static Consumer<FormMetadataPort> updateAsPulled(FormKey key) {
    return port -> {};
  }
}
