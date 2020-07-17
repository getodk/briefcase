package org.opendatakit.briefcase.model.form;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataQueries {
  public static Function<FormMetadataPort, Optional<Cursor>> lastCursorOf(FormKey key) {
    return port -> port.fetch(key).map(FormMetadata::getCursor);
  }

  public static Function<FormMetadataPort, Set<String>> submissionVersionsOf(FormKey key) {
    return port -> port.fetch(key).map(FormMetadata::getSubmissionVersions).orElse(new HashSet<>());
  }
}
