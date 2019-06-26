package org.opendatakit.briefcase.pull.aggregate;

import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;

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

  @Override
  public void storePrefs(FormStatus form, BriefcasePreferences prefs) {
    // Do nothing
  }
}
