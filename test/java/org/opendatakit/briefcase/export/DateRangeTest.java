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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;

public class DateRangeTest {
  private static final LocalDate START = LocalDate.of(2018, 1, 1);
  private static final LocalDate MIDDLE = START.plusMonths(6);
  private static final LocalDate END = LocalDate.of(2019, 1, 1);
  private static final DateRange range = DateRange.from(START, END);

  @Test
  public void knows_if_a_date_is_in_the_range() {
    assertThat(range.inRange(START.minusDays(1)), is(false));
    assertThat(range.inRange(START), is(true));
    assertThat(range.inRange(MIDDLE), is(true));
    assertThat(range.inRange(END), is(true));
    assertThat(range.inRange(END.plusDays(1)), is(false));
  }

  @Test
  public void admits_any_Temporal_instance_with_enough_precission() {
    assertThat(range.inRange(MIDDLE.atStartOfDay()), is(true));
    assertThat(range.inRange(MIDDLE.atStartOfDay().atOffset(ZoneOffset.UTC)), is(true));
    assertThat(range.inRange(MIDDLE.atStartOfDay().atZone(ZoneId.systemDefault())), is(true));
  }

  @Test(expected = DateTimeException.class)
  public void throws_with_Temporal_with_not_enough_precission() {
    range.inRange(Year.of(MIDDLE.getYear()));
  }
}