package org.opendatakit.briefcase.ui.export.components;

import java.util.Optional;

public enum TriStateBoolean {
  UNDETERMINED(Optional.empty()), TRUE(Optional.of(true)), FALSE(Optional.of(false));

  private final Optional<Boolean> booleanValue;

  TriStateBoolean(Optional<Boolean> booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Optional<Boolean> getBooleanValue() {
    return booleanValue;
  }
}
