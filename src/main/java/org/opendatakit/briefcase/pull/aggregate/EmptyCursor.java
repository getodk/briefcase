package org.opendatakit.briefcase.pull.aggregate;

import static org.opendatakit.briefcase.pull.aggregate.Cursor.Type.EMPTY;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EmptyCursor implements Cursor {
  @Override
  public Type getType() {
    return EMPTY;
  }

  @Override
  public String getValue() {
    return "";
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("type", EMPTY.getName());
    root.put("value", (String) null);
    return root;
  }

  @Override
  public int compareTo(Cursor o) {
    return -1;
  }

  public int hashCode() {
    return "".hashCode();
  }

  @Override
  public String toString() {
    return "EmptyCursor{}";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EmptyCursor;
  }
}
