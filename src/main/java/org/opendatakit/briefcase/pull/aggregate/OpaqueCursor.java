package org.opendatakit.briefcase.pull.aggregate;

import static org.opendatakit.briefcase.pull.aggregate.Cursor.Type.OPAQUE;

import java.util.Objects;
import java.util.Optional;

public class OpaqueCursor implements Cursor {
  private final String value;

  private OpaqueCursor(String value) {
    this.value = value;
  }

  public static Cursor from(String rawValue) {
    return new OpaqueCursor(Optional.ofNullable(rawValue).orElse(""));
  }

  @Override
  public Type getType() {
    return OPAQUE;
  }

  @Override
  public String getValue() {
    return Optional.ofNullable(value).map(Object::toString).orElse("");
  }

  @Override
  public boolean isEmpty() {
    return Optional.ofNullable(value).isEmpty();
  }

  @Override
  public int compareTo(Cursor o) {
    return value.compareTo(((OpaqueCursor) o).value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpaqueCursor that = (OpaqueCursor) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "OpaqueCursor{" +
        "value='" + value + '\'' +
        '}';
  }
}
