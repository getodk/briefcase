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

import java.time.OffsetDateTime;
import java.time.OffsetTime;

public class Iso8601Helpers {
  private static String normalizeOffset(String value) {
    char charAtMinus3 = value.charAt(value.length() - 3);
    if (value.endsWith("Z") || charAtMinus3 == ':')
      return value;
    if (charAtMinus3 == '+' || charAtMinus3 == '-')
      return value + ":00";
    return String.format("%s:%s", value.substring(0, 26), value.substring(26));
  }

  public static OffsetDateTime parseDateTime(String value) {
    return OffsetDateTime.parse(normalizeOffset(value));
  }

  public static OffsetTime parseTime(String value) {
    return OffsetTime.parse(normalizeOffset(value));
  }
}
