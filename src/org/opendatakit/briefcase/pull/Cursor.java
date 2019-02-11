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

package org.opendatakit.briefcase.pull;

import java.time.OffsetDateTime;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Iso8601Helpers;

public class Cursor implements Comparable<Cursor> {
  private final String value;
  private final OffsetDateTime lastUpdate;

  private Cursor(String value, OffsetDateTime lastUpdate) {
    this.value = value;
    this.lastUpdate = lastUpdate;
  }

  public static Cursor empty() {
    return new Cursor("", OffsetDateTime.parse("1980-01-01T00:00:00.000Z"));
  }

  public static Cursor from(String cursorXml) {
    OffsetDateTime lastUpdate = XmlElement.from(cursorXml)
        .findElement("attributeValue")
        .flatMap(XmlElement::maybeValue)
        .map(Iso8601Helpers::normalizeDateTime)
        .map(OffsetDateTime::parse)
        .orElseThrow(BriefcaseException::new);

    return new Cursor(cursorXml, lastUpdate);
  }

  public String get() {
    return value;
  }

  @Override
  public int compareTo(Cursor other) {
    return lastUpdate.compareTo(other.lastUpdate);
  }
}
