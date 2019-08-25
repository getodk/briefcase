package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor.Type.EMPTY;

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
