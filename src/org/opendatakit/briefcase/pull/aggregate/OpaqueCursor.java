package org.opendatakit.briefcase.pull.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
  public String getValue() {
    return Optional.ofNullable(value).map(Object::toString).orElse("");
  }

  @Override
  public boolean isEmpty() {
    return !Optional.ofNullable(value).isPresent();
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("type", Type.OPAQUE.getName());
    root.put("value", value);
    return root;
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
