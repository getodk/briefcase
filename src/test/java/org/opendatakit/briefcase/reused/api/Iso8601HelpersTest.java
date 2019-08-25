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

package org.opendatakit.briefcase.reused.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.Iso8601Helpers.parseDateTime;
import static org.opendatakit.briefcase.reused.api.Iso8601HelpersTest.TestType.DATETIME;
import static org.opendatakit.briefcase.reused.api.Iso8601HelpersTest.TestType.TIME;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class Iso8601HelpersTest {
  enum TestType {DATETIME, TIME}

  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public TestType testType;

  @Parameterized.Parameter(value = 2)
  public String input;

  @Parameterized.Parameter(value = 3)
  public Temporal expectedOutput;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Correct datetime format - Offset Z", DATETIME, "2010-01-01T00:00:00.000Z", OffsetDateTime.parse("2010-01-01T00:00:00.000Z")},
        {"Correct datetime format - Offset +00:00", DATETIME, "2010-01-01T00:00:00.000+00:00", OffsetDateTime.parse("2010-01-01T00:00:00.000+00:00")},
        {"Correct datetime format - Offset -00:00", DATETIME, "2010-01-01T00:00:00.000-00:00", OffsetDateTime.parse("2010-01-01T00:00:00.000-00:00")},
        {"Correct datetime format - Offset +03:00", DATETIME, "2010-01-01T00:00:00.000+03:00", OffsetDateTime.parse("2010-01-01T00:00:00.000+03:00")},
        {"Correct datetime format - Offset -03:00", DATETIME, "2010-01-01T00:00:00.000-03:00", OffsetDateTime.parse("2010-01-01T00:00:00.000-03:00")},
        {"Wrong datetime format - Offset +0030", DATETIME, "2010-01-01T00:00:00.000+0030", OffsetDateTime.parse("2010-01-01T00:00:00.000+00:30")},
        {"Wrong datetime format - Offset -0030", DATETIME, "2010-01-01T00:00:00.000-0030", OffsetDateTime.parse("2010-01-01T00:00:00.000-00:30")},
        {"Wrong datetime format - Offset +01", DATETIME, "2010-01-01T00:00:00.000+01", OffsetDateTime.parse("2010-01-01T00:00:00.000+01:00")},
        {"Wrong time format - Offset +01", TIME, "08:08:00.000+01", OffsetTime.parse("08:08:00.000+01:00")}
    });
  }

  @Test
  public void normalizes_and_parses_incoming_datetimes_and_times_with_minor_defects_in_their_offsets() {
    assertThat(testType == DATETIME ? parseDateTime(input) : Iso8601Helpers.parseTime(input), is(expectedOutput));
  }
}
