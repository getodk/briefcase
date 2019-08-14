package org.opendatakit.briefcase.pull.aggregate;

public class EmptyCursor implements Cursor<Cursor> {
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

}
