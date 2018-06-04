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

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * This class represents a Date range and offers an API to query if
 * a given {@link Temporal} is within it.
 */
class DateRange {
  private final Optional<LocalDate> start;
  private final Optional<LocalDate> end;

  DateRange(Optional<LocalDate> start, Optional<LocalDate> end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Factory that creates a {@link DateRange} instance with present start
   * and end members.
   *
   * @param start the {@link LocalDate} start of the {@link DateRange}
   * @param end   the {@link LocalDate} end of the {@link DateRange}
   * @return a new {@link DateRange} instance
   */
  static DateRange from(LocalDate start, LocalDate end) {
    return new DateRange(
        Optional.of(start),
        Optional.of(end)
    );
  }

  /**
   * Returns whether a given {@link Temporal} is wihtin the date range
   * this instance of {@link DateRange} represents.
   * <p>
   * Both the start and end of the date range are inclusive.
   * <p>
   * The given {@link Temporal} has to have a resolution of at least
   * {@link java.time.temporal.ChronoUnit#DAYS}
   *
   * @param temporal the {@link Temporal} to check against this date range
   * @return true if the given {@link Temporal} is within this date range. False otherwise.
   */
  public boolean contains(Temporal temporal) {
    LocalDate targetDate = LocalDate.from(temporal);
    return start.map(ld -> !ld.isAfter(targetDate)).orElse(true)
        && end.map(ld -> !ld.isBefore(targetDate)).orElse(true);
  }
}
