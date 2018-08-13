package org.opendatakit.briefcase.ui.export.components;

import java.util.Optional;

public enum Value {
  INHERIT(Optional.empty()), YES(Optional.of(true)), NO(Optional.of(false));

  private final Optional<Boolean> booleanValue;

  Value(Optional<Boolean> booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Optional<Boolean> getBooleanValue() {
    return booleanValue;
  }
}
