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

package org.opendatakit.briefcase.matchers;

import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PathMatchers {

  public static Matcher<Path> exists() {
    return new TypeSafeMatcher<Path>() {
      @Override
      protected boolean matchesSafely(Path item) {
        return Files.exists(item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("exists");
      }

      @Override
      protected void describeMismatchSafely(Path item, Description mismatchDescription) {
        mismatchDescription.appendText("doesn't exist");
      }
    };
  }
}
