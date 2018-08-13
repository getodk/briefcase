package org.opendatakit.briefcase.reused;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public enum TriStateBoolean {
  UNDETERMINED(null), TRUE(true), FALSE(false);

  private static final List<String> UNDETERMINED_VALUES = Arrays.asList("UNDETERMINED", "INHERIT");
  private static final List<String> TRUE_VALUES = Arrays.asList("TRUE", "PULL", "EXPORT_MEDIA");
  private static final List<String> FALSE_VALUES = Arrays.asList("FALSE", "DONT_PULL", "DONT_EXPORT_MEDIA");

  private final Boolean booleanValue;

  TriStateBoolean(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public static TriStateBoolean of(boolean value) {
    return value ? TRUE : FALSE;
  }

  public Optional<Boolean> maybeGetBooleanValue() {
    return Optional.ofNullable(booleanValue);
  }

  public boolean getBooleanValue() {
    return Objects.requireNonNull(booleanValue, "Can't get a boolean value from an UNDETERMINED TriStateBoolean");
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

  public static boolean isUndetermined(TriStateBoolean value) {
    return value == UNDETERMINED;
  }

  public static boolean isNotUndetermined(TriStateBoolean value) {
    return value != UNDETERMINED;
  }

  @Override
  public String toString() {
    return "Boolean(" + booleanValue + ')';
  }
}
