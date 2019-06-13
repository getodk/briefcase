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

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.Iso8601Helpers;

/**
 * This class stores information about a cursor to a list of remote submission
 * instance IDs, or "resumptionCursor" as described in the <a href="https://docs.opendatakit.org/briefcase-api/#returned-document">Briefcase Aggregate API docs</a>
 * <p>
 * The contents of a Cursor are basically a date ({@link #lastUpdate} field) and
 * a submission instanceID ({@link #lastReturnedValue} field), which are used by
 * Aggregate to define the lower bound of a page of submission instanceIDs (also
 * called batch or chunk in the documentation). The upper bound is a defined by
 * another parameter that's not part of the cursor.
 * <p>
 * The date ({@link #lastUpdate} field) in a cursor is related to the last update
 * date of a submission stored in Aggregate's database, not to be mistaken with
 * the completion or submission dates, which can be different.
 * <p>
 * The submission instanceID of a Cursor ({@link #lastReturnedValue} field) is used
 * to further filter the contents of a submission instanceID page when the existing
 * submissions from the provided date don't fit in the same page.
 */
public class Cursor implements Comparable<Cursor> {
  /**
   * This date is used only to compare Cursors that might have an empty value in lastUpdate
   */
  private static final OffsetDateTime SOME_OLD_DATE = OffsetDateTime.parse("2010-01-01T00:00:00.000Z");
  // TODO v2.0 Use a better name, like xml
  private final String value;
  private final Optional<OffsetDateTime> lastUpdate;
  private final Optional<String> lastReturnedValue;

  private Cursor(String value, Optional<OffsetDateTime> lastUpdate, Optional<String> lastReturnedValue) {
    this.value = value;
    this.lastUpdate = lastUpdate;
    this.lastReturnedValue = lastReturnedValue;
  }

  public static Cursor empty() {
    return new Cursor("", Optional.empty(), Optional.empty());
  }

  /**
   * Parses the provided cursor xml document and returns a new Cursor instance.
   */
  public static Cursor from(String cursorXml) {
    if (cursorXml.isEmpty())
      return Cursor.empty();

    XmlElement root = XmlElement.from(cursorXml);

    Optional<OffsetDateTime> lastUpdate = root
        .findElement("attributeValue")
        .flatMap(XmlElement::maybeValue)
        .map(Iso8601Helpers::parseDateTime);

    Optional<String> lastReturnedValue = root
        .findElement("uriLastReturnedValue")
        .flatMap(XmlElement::maybeValue);

    return new Cursor(cursorXml, lastUpdate, lastReturnedValue);
  }

  /**
   * Returns a synthetic Cursor instance with the provided values.
   */
  public static Cursor of(OffsetDateTime lastUpdate, String lastReturnedValue) {
    String cursorXml = String.format("<cursor xmlns=\"http://www.opendatakit.org/cursor\">" +
            "<attributeName>_LAST_UPDATE_DATE</attributeName>" +
            "<attributeValue>%s</attributeValue>" +
            "<uriLastReturnedValue>%s</uriLastReturnedValue>" +
            "<isForwardCursor>true</isForwardCursor>" +
            "</cursor>",
        lastUpdate.format(ISO_OFFSET_DATE_TIME),
        lastReturnedValue
    );
    return new Cursor(cursorXml, Optional.of(lastUpdate), Optional.of(lastReturnedValue));
  }

  /**
   * Returns a synthetic Cursor instance with the provided values.
   */
  public static Cursor of(LocalDate date) {
    OffsetDateTime lastUpdate = date.atStartOfDay().atOffset(ZoneOffset.UTC);
    String cursorXml = String.format("<cursor xmlns=\"http://www.opendatakit.org/cursor\">" +
            "<attributeName>_LAST_UPDATE_DATE</attributeName>" +
            "<attributeValue>%s</attributeValue>" +
            "<uriLastReturnedValue/>" +
            "<isForwardCursor>true</isForwardCursor>" +
            "</cursor>",
        lastUpdate.format(ISO_OFFSET_DATE_TIME)
    );
    return new Cursor(cursorXml, Optional.of(lastUpdate), Optional.empty());
  }

  /**
   * Returns a synthetic Cursor instance with the provided values.
   */
  public static Cursor of(LocalDate date, String lastReturnedValue) {
    return of(date.atStartOfDay().atOffset(ZoneOffset.UTC), lastReturnedValue);
  }

  // TODO v2.0 Use a better name, like getXml();
  public String get() {
    return value;
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  @Override
  public int compareTo(Cursor other) {
    // Hacky way to adapt to values that might have empty lastUpdate
    // members that provides valid comparison results for our purposes
    return lastUpdate.orElse(SOME_OLD_DATE).compareTo(other.lastUpdate.orElse(SOME_OLD_DATE));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Cursor cursor = (Cursor) o;
    return lastUpdate.equals(cursor.lastUpdate) &&
        lastReturnedValue.equals(cursor.lastReturnedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastUpdate, lastReturnedValue);
  }
}
