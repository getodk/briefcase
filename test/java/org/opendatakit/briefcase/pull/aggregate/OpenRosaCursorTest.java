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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.FormStatusBuilder.buildFormStatus;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.InMemoryPreferences;

public class OpenRosaCursorTest {

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

  @Test
  public void stores_and_reads_using_prefs() {
    BriefcasePreferences testBriefcasePrefs = new BriefcasePreferences(InMemoryPreferences.empty());
    FormStatus form = buildFormStatus(1);
    Cursor originalCursor = Cursor.from(buildCursorXml("2019-01-01T00:00:00.000Z", "1234"));

    originalCursor.storePrefs(form, testBriefcasePrefs);
    assertThat(testBriefcasePrefs.keys(), hasSize(2));

    Optional<Cursor> actualCursor = Cursor.readPrefs(form, testBriefcasePrefs);
    assertThat(actualCursor, isPresentAndIs(originalCursor));
  }

  @Test
  public void cleans_all_cursors_stored_in_prefs() {
    BriefcasePreferences testBriefcasePrefs = new BriefcasePreferences(InMemoryPreferences.empty());
    // Store something we don't want to be removed
    testBriefcasePrefs.put("dummy key", "dummy value");
    Cursor.from(buildCursorXml("2019-01-01T00:00:00.000Z", "1")).storePrefs(buildFormStatus(1), testBriefcasePrefs);
    Cursor.from(buildCursorXml("2019-01-01T00:00:00.000Z", "2")).storePrefs(buildFormStatus(2), testBriefcasePrefs);
    Cursor.from(buildCursorXml("2019-01-01T00:00:00.000Z", "3")).storePrefs(buildFormStatus(3), testBriefcasePrefs);
    assertThat(testBriefcasePrefs.keys(), hasSize(7));

    Cursor.cleanAllPrefs(testBriefcasePrefs);

    assertThat(testBriefcasePrefs.keys(), hasSize(1));
  }
}
