/*
 * Copyright (C) 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.pull.aggregate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores information about a cursor to a list of remote submission
 * instance IDs, or "resumptionCursor" as described in the <a href="https://docs.opendatakit.org/briefcase-api/#returned-document">Briefcase Aggregate API docs</a>
 * <p>
 * OpenRosa specifies that cursors should be opaque, but to support the "start from last"
 * and "start from date" features, we need to try to parse it and even create artificial
 * cursors.
 */
public interface Cursor<T extends Cursor> extends Comparable<T> {
  Logger log = LoggerFactory.getLogger(Cursor.class);
  String LAST_CURSOR_PREFERENCE_KEY_SUFFIX = "-last-cursor";
  String LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX = "-last-cursor-type";

  static Optional<Cursor> readPrefs(FormStatus form, BriefcasePreferences prefs) {
    Type type = prefs.nullSafeGet(form.getFormId() + LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX)
        .map(Type::from)
        .orElse(Type.AGGREGATE);
    return prefs.nullSafeGet(form.getFormId() + LAST_CURSOR_PREFERENCE_KEY_SUFFIX)
        .map(type::create);
  }

  static void cleanAllPrefs(BriefcasePreferences prefs) {
    prefs.keys().stream()
        .filter(key -> key.endsWith(LAST_CURSOR_PREFERENCE_KEY_SUFFIX) || key.endsWith(LAST_CURSOR_TYPE_PREFERENCE_KEY_SUFFIX))
        .forEach(prefs::remove);
  }

  static Cursor empty() {
    return new EmptyCursor();
  }

  String getValue();

  boolean isEmpty();

  void storePrefs(FormStatus form, BriefcasePreferences prefs);

  /**
   * Create a cursor that would start pulling from the provided date in Aggregate servers.
   */
  static AggregateCursor of(LocalDate lastUpdateDate) {
    return AggregateCursor.of(lastUpdateDate);
  }

  /**
   * Create a cursor that would start pulling from the provided date in Aggregate servers.
   */
  static Cursor of(OffsetDateTime lastUpdateDateTime, String uid) {
    return AggregateCursor.of(lastUpdateDateTime, uid);
  }

  static Cursor from(String value) {
    return firstNonFailing(
        () -> AggregateCursor.from(value),
        () -> OnaCursor.from(value),
        () -> OpaqueCursor.from(value)
    ).orElseThrow(() -> new BriefcaseException("Unknown cursor format"));
  }

  @SafeVarargs
  static Optional<Cursor> firstNonFailing(Supplier<Cursor>... suppliers) {
    for (Supplier<Cursor> supplier : suppliers) {
      try {
        return Optional.of(supplier.get());
      } catch (Throwable t) {
        // Ignore exception
      }
    }
    return Optional.empty();
  }


  enum Type {

    AGGREGATE("aggregate", AggregateCursor::from),
    ONA("ona", OnaCursor::from),
    OPAQUE("opaque", OpaqueCursor::from);

    private final String name;
    private final Function<String, Cursor> factory;

    Type(String name, Function<String, Cursor> factory) {
      this.name = name;
      this.factory = factory;
    }

    static Type from(String type) {
      if (type.equals("aggregate"))
        return AGGREGATE;
      if (type.equals("ona"))
        return ONA;
      return OPAQUE;
    }

    public Cursor create(String rawValue) {
      return factory.apply(rawValue);
    }

    public String getName() {
      return name;
    }
  }

}
