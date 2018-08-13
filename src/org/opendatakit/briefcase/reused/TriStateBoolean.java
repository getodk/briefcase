package org.opendatakit.briefcase.reused;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TriStateBoolean {
  UNDETERMINED(Optional.empty()), TRUE(Optional.of(true)), FALSE(Optional.of(false));

  private static final List<String> UNDETERMINED_VALUES = Arrays.asList("UNDETERMINED", "INHERIT");
  private static final List<String> TRUE_VALUES = Arrays.asList("TRUE", "PULL", "EXPORT_MEDIA");
  private static final List<String> FALSE_VALUES = Arrays.asList("FALSE", "DONT_PULL", "DONT_EXPORT_MEDIA");

  private final Optional<Boolean> booleanValue;

  TriStateBoolean(Optional<Boolean> booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Optional<Boolean> getBooleanValue() {
    return booleanValue;
  }

  public static TriStateBoolean from(String value) {
    if (UNDETERMINED_VALUES.contains(value))
      return UNDETERMINED;
    if (TRUE_VALUES.contains(value))
      return TRUE;
    if (FALSE_VALUES.contains(value))
      return FALSE;
    throw new IllegalArgumentException("Unsupported value " + value);
  }
}
