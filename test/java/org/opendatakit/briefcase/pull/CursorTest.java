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

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.Test;

public class CursorTest {

  public static String buildCursorXml(OffsetDateTime lastUpdate) {
    return buildCursorXml(lastUpdate, UUID.randomUUID().toString());
  }

  public static String buildCursorXml(OffsetDateTime lastUpdate, String lastId) {
    return buildCursorXml(lastUpdate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), lastId);
  }

  public static String buildCursorXml(String lastUpdate) {
    return buildCursorXml(lastUpdate, UUID.randomUUID().toString());
  }

  public static String buildCursorXml(String lastUpdate, String lastId) {
    return "" +
        "<cursor xmlns=\"http://www.opendatakit.org/cursor\">\n" +
        "<attributeName>_LAST_UPDATE_DATE</attributeName>\n" +
        "<attributeValue>" + lastUpdate + "</attributeValue>\n" +
        "<uriLastReturnedValue>" + lastId + "</uriLastReturnedValue>\n" +
        "<isForwardCursor>true</isForwardCursor>\n" +
        "</cursor>" +
        "";
  }

  @Test
  public void fixes_dates_while_parsing_cursors() {
    assertThat(
        Cursor.from(buildCursorXml("2010-01-01T00:00:00.000+0800")),
        lessThan(Cursor.from(buildCursorXml("2010-01-01T00:00:00.000+03")))
    );
  }
}
