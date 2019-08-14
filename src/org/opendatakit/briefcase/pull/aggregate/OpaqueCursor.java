package org.opendatakit.briefcase.pull.aggregate;

import java.util.Optional;

public class OpaqueCursor implements Cursor<OpaqueCursor> {

  private final String value;

  private OpaqueCursor(String value) {
    this.value = value;
  }

  public static Cursor from(String rawValue) {
    return new OpaqueCursor(Optional.ofNullable(rawValue).orElse(""));
  }

  @Override
  public String getValue() {
    return Optional.ofNullable(value).map(Object::toString).orElse("");
  }

  @Override
  public boolean isEmpty() {
    return !Optional.ofNullable(value).isPresent();
  }

  @Override
  public int compareTo(OpaqueCursor o) {
    return Optional.ofNullable(value).orElse("")
        .compareTo(Optional.ofNullable(o.value).orElse(""));
  }
}
