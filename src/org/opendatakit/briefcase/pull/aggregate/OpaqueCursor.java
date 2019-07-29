package org.opendatakit.briefcase.pull.aggregate;

import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;

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
  public void storePrefs(FormStatus form, BriefcasePreferences prefs) {
    prefs.put(form.getFormId() + LAST_CURSOR_PREFERENCE_KEY_SUFFIX, getValue());
    prefs.put(form.getFormId() + LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX, Type.OPAQUE.getName());
  }

  @Override
  public int compareTo(OpaqueCursor o) {
    return Optional.ofNullable(value).orElse("")
        .compareTo(Optional.ofNullable(o.value).orElse(""));
  }
}
