package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;

/**
 * This class will gather all the legacy Java preferences accessing
 * code to make it easier to refactor and migrate to other data storing schemes.
 */
public class LegacyPrefs {

  private static final String LAST_CURSOR_PREFERENCE_KEY_SUFFIX = "-last-cursor";
  private static final String LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX = "-last-cursor-type";

  public static Optional<Cursor> readCursor(String formId) {
    return readCursor(formId, BriefcasePreferences.appScoped());
  }

  private static Optional<Cursor> readCursor(String formId, BriefcasePreferences prefs) {
    Cursor.Type type = prefs.nullSafeGet(formId + LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX)
        .map(Cursor.Type::from)
        .orElse(Cursor.Type.AGGREGATE);
    return prefs.nullSafeGet(formId + LAST_CURSOR_PREFERENCE_KEY_SUFFIX)
        .map(type::create);
  }

}
