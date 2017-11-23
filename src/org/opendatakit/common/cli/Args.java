package org.opendatakit.common.cli;

import java.util.Map;
import java.util.Optional;

public class Args {
  private final Map<Param, String> keyValues;

  Args(Map<Param, String> keyValues) {
    this.keyValues = keyValues;
  }

  public boolean has(Param<?> key) {
    return keyValues.containsKey(key);
  }

  public <T> Optional<T> getOptional(Param<T> key) {
    return Optional.ofNullable(keyValues.get(key))
        .map(key::map);
  }

  public <T> T get(Param<T> key) {
    return getOptional(key)
        .orElseThrow(() -> new IllegalArgumentException("No option " + key + " has been received"));
  }

  public <T> T getOrNull(Param<T> key) {
    return getOptional(key).orElse(null);
  }
}
