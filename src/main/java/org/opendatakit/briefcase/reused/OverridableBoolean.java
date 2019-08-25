package org.opendatakit.briefcase.reused;

import static org.opendatakit.briefcase.reused.OptionalProduct.firstPresent;
import static org.opendatakit.briefcase.reused.TriStateBoolean.UNDETERMINED;
import static org.opendatakit.briefcase.reused.TriStateBoolean.isNotUndetermined;
import static org.opendatakit.briefcase.reused.TriStateBoolean.isUndetermined;

import java.util.Objects;
import java.util.Optional;

public class OverridableBoolean {
  public static final OverridableBoolean TRUE = new OverridableBoolean(Optional.of(true), UNDETERMINED);
  public static final OverridableBoolean FALSE = new OverridableBoolean(Optional.of(false), UNDETERMINED);
  private final Optional<Boolean> value;
  private final TriStateBoolean overrideValue;

  private OverridableBoolean(Optional<Boolean> value, TriStateBoolean overrideValue) {
    this.value = value;
    this.overrideValue = overrideValue;
  }

  public static OverridableBoolean empty() {
    return new OverridableBoolean(Optional.empty(), UNDETERMINED);
  }

  public static OverridableBoolean of(boolean value) {
    return new OverridableBoolean(Optional.of(value), UNDETERMINED);
  }

  public boolean get(boolean defaultValue) {
    return value.orElse(defaultValue);
  }

  public OverridableBoolean set(Boolean value) {
    return new OverridableBoolean(Optional.of(value), overrideValue);
  }

  public OverridableBoolean overrideWith(TriStateBoolean overrideValue) {
    return new OverridableBoolean(this.value, overrideValue);
  }

  public TriStateBoolean getOverride() {
    return overrideValue;
  }

  public boolean resolve(boolean defaultValue) {
    return overrideValue == UNDETERMINED
        ? value.orElse(defaultValue)
        : overrideValue.getBooleanValue();
  }

  public static OverridableBoolean from(String serializedValue) {
    String[] parts = serializedValue.split(",");
    try {
      return new OverridableBoolean(
          Optional.of(parts[0].trim()).filter(s -> !s.isEmpty()).map(Boolean::parseBoolean),
          Optional.of(parts[1].trim()).map(TriStateBoolean::from).orElse(UNDETERMINED)
      );
    } catch (Throwable t) {
      return empty();
    }
  }

  public String serialize() {
    return value.map(Object::toString).orElse("") + "," + overrideValue.name();
  }

  public OverridableBoolean fallingBackTo(OverridableBoolean base) {
    return new OverridableBoolean(
        firstPresent(value, base.value),
        isNotUndetermined(overrideValue) ? overrideValue : base.overrideValue
    );
  }

  public boolean isEmpty() {
    return value.isEmpty() && isUndetermined(overrideValue);
  }

  @Override
  public String toString() {
    return "Boolean(" + value.map(Object::toString).orElse("") + ", " + overrideValue + ')';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OverridableBoolean that = (OverridableBoolean) o;
    return Objects.equals(value, that.value) &&
        overrideValue == that.overrideValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, overrideValue);
  }
}
