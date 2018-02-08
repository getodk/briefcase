package org.opendatakit.briefcase.export;

import java.util.Optional;
import java.util.stream.Stream;

public enum PullBeforeOverrideOption {
  INHERIT("Use defaults", null), PULL("Pull before export", true), DONT_PULL("Do not pull before export", false);

  private final String label;
  private final Optional<Boolean> value;

  PullBeforeOverrideOption(String label, Boolean value) {
    this.label = label;
    this.value = Optional.ofNullable(value);
  }

  public static PullBeforeOverrideOption from(String name) {
    return Stream.of(values())
        .filter(value -> value.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown PullBeforeOverrideOption value " + name));
  }

  public static PullBeforeOverrideOption from(Optional<Boolean> maybeValue) {
    return maybeValue.map(value -> value ? PULL : DONT_PULL).orElse(INHERIT);
  }

  public Optional<Boolean> asBoolean() {
    return value;
  }

  @Override
  public String toString() {
    return label;
  }
}
