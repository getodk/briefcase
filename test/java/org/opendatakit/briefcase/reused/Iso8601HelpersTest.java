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

package org.opendatakit.briefcase.reused;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.Iso8601Helpers.normalizeDateTime;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class Iso8601HelpersTest {
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public String input;

  @Parameterized.Parameter(value = 2)
  public String expectedOutput;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Correct format - Offset Z", "2010-01-01T00:00:00.000Z", "2010-01-01T00:00:00.000Z"},
        {"Correct format - Offset +00:00", "2010-01-01T00:00:00.000+00:00", "2010-01-01T00:00:00.000+00:00"},
        {"Correct format - Offset -00:00", "2010-01-01T00:00:00.000-00:00", "2010-01-01T00:00:00.000-00:00"},
        {"Correct format - Offset +03:00", "2010-01-01T00:00:00.000+03:00", "2010-01-01T00:00:00.000+03:00"},
        {"Correct format - Offset -03:00", "2010-01-01T00:00:00.000-03:00", "2010-01-01T00:00:00.000-03:00"},
        {"Wrong format - Offset +0030", "2010-01-01T00:00:00.000+0030", "2010-01-01T00:00:00.000+00:30"},
        {"Wrong format - Offset -0030", "2010-01-01T00:00:00.000-0030", "2010-01-01T00:00:00.000-00:30"},
        {"Wrong format - Offset +01", "2010-01-01T00:00:00.000+01", "2010-01-01T00:00:00.000+01:00"},
    });
  }

  @Test
  public void normalizes_iso_8601_datetimes_and_they_can_be_parsed() {
    assertThat(normalizeDateTime(input), is(expectedOutput));
    assertThat(OffsetDateTime.parse(normalizeDateTime(input)), is(OffsetDateTime.parse(expectedOutput)));
  }
}
