/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.export;

import static java.lang.Math.abs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.IntStream;
import org.junit.Test;

public class DateRangeTest {

  @Test
  public void knows_if_a_date_is_in_the_range() {
    runInOffset(0, () -> {
      DateRange range = DateRange.from(LocalDate.parse("2018-01-01"), LocalDate.parse("2019-01-01"));
      assertThat(range.contains(OffsetDateTime.parse("2017-12-31T00:00:00+00:00")), is(false));
      assertThat(range.contains(OffsetDateTime.parse("2018-01-01T00:00:00+00:00")), is(true));
      assertThat(range.contains(OffsetDateTime.parse("2018-06-01T00:00:00+00:00")), is(true));
      assertThat(range.contains(OffsetDateTime.parse("2019-01-01T00:00:00+00:00")), is(true));
      assertThat(range.contains(OffsetDateTime.parse("2019-01-02T00:00:00+00:00")), is(false));
      assertThat(range.contains(OffsetDateTime.parse("2019-01-02T01:00:00+02:00")), is(false));
    });
  }

  @Test
  public void end_date_is_inclusive() {
    runInOffset(0, () -> {
      DateRange range = DateRange.from(LocalDate.parse("2018-01-01"), LocalDate.parse("2019-01-01"));
      // It includes all the end date, including all its nanoseconds until midnight
      assertThat(range.contains(OffsetDateTime.parse("2019-01-01T23:59:59.999999999+00:00")), is(true));
    });
  }

  @Test
  public void ignores_the_offset_even_if_the_local_date_was_inside_the_range() {
    runInOffset(0, () -> {
      DateRange range = DateRange.from(LocalDate.parse("2018-01-01"), LocalDate.parse("2019-01-01"));
      // This date would be the local date/time 2019-01-01T23:00:00+00:00, which is included
      assertThat(range.contains(OffsetDateTime.parse("2019-01-02T01:00:00+02:00")), is(false));
    });
  }

  @Test
  public void comprehensive_offset_test() {
    // Al possible local offsets
    IntStream.range(-12, 12).forEach(localOffset -> runInOffset(localOffset, () -> {
      // All possible target offsets
      IntStream.range(-12, 12).forEach(targetOffset -> {
        DateRange range = DateRange.from(LocalDate.parse("2016-06-05"), LocalDate.parse("2017-06-05"));
        String sign = targetOffset < 0 ? "-" : "+";
        assertThat(range.contains(OffsetDateTime.parse(String.format("2016-06-04T18:00:00%s%02d:00", sign, abs(targetOffset)))), is(false));
        assertThat(range.contains(OffsetDateTime.parse(String.format("2016-06-05T06:00:00%s%02d:00", sign, abs(targetOffset)))), is(true));
        assertThat(range.contains(OffsetDateTime.parse(String.format("2017-06-05T18:00:00%s%02d:00", sign, abs(targetOffset)))), is(true));
        assertThat(range.contains(OffsetDateTime.parse(String.format("2017-06-06T06:00:00%s%02d:00", sign, abs(targetOffset)))), is(false));
      });
    }));
  }

  private void runInOffset(int hours, Runnable block) {
    ZoneOffset zoneOffset = ZoneOffset.ofHours(hours);
    TimeZone backup = TimeZone.getDefault();
    String zoneId = TimeZone.getAvailableIDs(zoneOffset.getTotalSeconds() * 1000)[0];
    TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    block.run();
    TimeZone.setDefault(backup);
  }
}