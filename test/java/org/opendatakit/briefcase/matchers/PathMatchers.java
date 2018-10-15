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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PathMatchers {

  public static Matcher<Path> exists() {
    return new TypeSafeMatcher<Path>() {
      private Path item;

      @Override
      protected boolean matchesSafely(Path item) {
        this.item = item;
        return Files.exists(item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendValue(item).appendText(" exists");
      }

      @Override
      protected void describeMismatchSafely(Path item, Description mismatchDescription) {
        mismatchDescription.appendText("doesn't exist");
      }
    };
  }

  public static Matcher<Path> fileContains(String content) {
    return new TypeSafeMatcher<Path>() {
      private String actualContents;

      @Override
      protected boolean matchesSafely(Path item) {
        try {
          actualContents = new String(Files.readAllBytes(item), UTF_8);
          return actualContents.contains(content);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a file containing ").appendValue(content);
      }

      @Override
      protected void describeMismatchSafely(Path item, Description mismatchDescription) {
        mismatchDescription.appendText("was a file containing ").appendValue(actualContents);
      }
    };
  }

  public static Matcher<Path> fileExactlyContains(String content) {
    return new TypeSafeMatcher<Path>() {
      private String actualContents;

      @Override
      protected boolean matchesSafely(Path item) {
        try {
          actualContents = new String(Files.readAllBytes(item), UTF_8);
          return actualContents.equals(content);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a file containing ").appendValue(content);
      }

      @Override
      protected void describeMismatchSafely(Path item, Description mismatchDescription) {
        mismatchDescription.appendText("was a file containing ").appendValue(actualContents);
      }
    };
  }
}
