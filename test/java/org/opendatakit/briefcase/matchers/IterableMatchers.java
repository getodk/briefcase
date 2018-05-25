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

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IterableMatchers {
  public static <T> Matcher<Iterable<T>> containsAtLeast(T... elements) {
    return new TypeSafeMatcher<Iterable<T>>() {
      @Override
      protected boolean matchesSafely(Iterable<T> item) {

        return StreamSupport.stream(item.spliterator(), false)
            .collect(Collectors.toList())
            .containsAll(Arrays.asList(elements));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("contains all elements: ").appendValueList("{", ", ", "}", elements);
      }
    };
  }
}
