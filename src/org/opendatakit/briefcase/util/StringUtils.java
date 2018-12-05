/*
 * Copyright (C) 2017 University of Washington.
 * Copyright (C) 2018 Nafundi.
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

package org.opendatakit.briefcase.util;

public class StringUtils {

  private static final String INPUTS_COMPOSED_ONLY_BY_DOTS_AND_SLASHES = "^[./]+$";
  private static final String TRAILING_DOTS_AND_SPACES = "[. ]+$";
  private static final String WINDOWSY_RESERVED_NAMES = "^(con|prn|aux|nul|com[0-9]|lpt[0-9]|CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(\\..*)?";
  private static final String CONTROL_CHARS = "[\\x00-\\x1f\\x80-\\x9f]";
  private static final String SOME_PUNCTUATION_CHARS = "[/?<>\\\\*|\":]";

  public static boolean nullOrEmpty(String text) {
    return text == null || text.trim().length() == 0;
  }

  public static String stripIllegalChars(String input) {
    if (input == null)
      return null;
    return input
        .replaceAll(INPUTS_COMPOSED_ONLY_BY_DOTS_AND_SLASHES, "")
        .replaceAll(TRAILING_DOTS_AND_SPACES, "")
        // Apparently, the library Central's using doesn't trim heading dots and spaces
        // .replaceAll("^[. ]+", "")
        .replaceAll(WINDOWSY_RESERVED_NAMES, "")
        .replaceAll(CONTROL_CHARS, "")
        .replaceAll(SOME_PUNCTUATION_CHARS, "");
  }
}
