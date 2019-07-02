package org.opendatakit.briefcase.pull.aggregate;

import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;

/**
 * This class represents Ona's implementation of a Cursor.
 * <p>
 * It's much simpler than the {@link AggregateCursor} because
 * it only stores a numeric ID.
 * <p>
 * This makes it possible to support the "start from last" feature,
 * but not the "start from date", since Ona cursors don't include
 * any date.
 */
public class OnaCursor implements Cursor<OnaCursor> {
  private final Optional<Long> value;

  private OnaCursor(Optional<Long> value) {
    this.value = value;
  }

  public static Cursor from(String rawValue) {
    return new OnaCursor(Optional.ofNullable(rawValue).map(Long::parseLong));
  }

  @Override
  public int compareTo(OnaCursor o) {
    return Long.compare(value.orElse(-1L), o.value.orElse(-1L));
  }

  @Override
  public String getValue() {
    return value.map(Object::toString).orElse("");
  }

  @Override
  public boolean isEmpty() {
    return !value.isPresent();
  }

  @Override
  public void storePrefs(FormStatus form, BriefcasePreferences prefs) {
    prefs.put(form.getFormId() + LAST_CURSOR_PREFERENCE_KEY_SUFFIX, getValue());
    prefs.put(form.getFormId() + LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX, Type.ONA.getName());
  }

}
