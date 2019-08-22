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

package org.opendatakit.briefcase.reused.http;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.internal.SelfDescribingValue;

public class RequestSpyMatchers {
  public static <T> TypeSafeMatcher<RequestSpy<T>> hasBeenCalled() {
    return new TypeSafeMatcher<RequestSpy<T>>() {
      @Override
      protected boolean matchesSafely(RequestSpy<T> item) {
        return item.called;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has been called");
      }

      @Override
      protected void describeMismatchSafely(RequestSpy<T> item, Description mismatchDescription) {
        mismatchDescription.appendValue(item.request).appendText(" hasn't been called");
      }
    };
  }

  public static <T> TypeSafeMatcher<RequestSpy<T>> isMultipart() {
    return new TypeSafeMatcher<RequestSpy<T>>() {
      @Override
      protected boolean matchesSafely(RequestSpy<T> item) {
        return item.request.isMultipart();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("is multipart");
      }
    };
  }

  public static <T> TypeSafeMatcher<RequestSpy<T>> hasPartNames(String... parts) {
    return new TypeSafeMatcher<RequestSpy<T>>() {
      @Override
      protected boolean matchesSafely(RequestSpy<T> item) {
        List<String> partNames = item.request.multipartMessages
            .stream().map(mm -> mm.name)
            .collect(toList());
        return partNames.size() == parts.length
            && partNames.containsAll(Arrays.asList(parts));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has parts with names ").appendValue(parts);
      }

      @Override
      protected void describeMismatchSafely(RequestSpy<T> item, Description mismatchDescription) {
        List<String> partNames = item.request.multipartMessages
            .stream().map(mm -> mm.name)
            .collect(toList());
        mismatchDescription.appendText("has parts with names ").appendValue(partNames);
      }
    };
  }

  public static <T> TypeSafeMatcher<RequestSpy<T>> hasPart(String name, String contentType, String attachmentName) {
    return new TypeSafeMatcher<RequestSpy<T>>() {
      @Override
      protected boolean matchesSafely(RequestSpy<T> item) {
        return item.request.multipartMessages
            .stream()
            .anyMatch(mm -> mm.name.equals(name)
                & mm.contentType.equals(contentType)
                && mm.attachmentName.equals(attachmentName));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has part ").appendValue(String.format("(%s, %s, %s)", name, contentType, attachmentName));
      }

      @Override
      protected void describeMismatchSafely(RequestSpy<T> item, Description mismatchDescription) {
        List<SelfDescribingValue> parts = item.request.multipartMessages
            .stream()
            .map(SelfDescribingValue::new)
            .collect(toList());
        mismatchDescription.appendText("has parts ").appendList("", ", ", "", parts);
      }
    };
  }

  public static <T> TypeSafeMatcher<RequestSpy<T>> hasBody(byte[] expectedBody) {
    return hasBody(new String(expectedBody));
  }

  public static <T> TypeSafeMatcher<RequestSpy<T>> hasBody(String expectedBody) {
    return new TypeSafeMatcher<RequestSpy<T>>() {
      @Override
      protected boolean matchesSafely(RequestSpy<T> item) {
        return item.readBody().equals(expectedBody);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has body of ").appendText(expectedBody);
      }

      @Override
      protected void describeMismatchSafely(RequestSpy<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("has body of ").appendText(item.readBody());
      }
    };
  }
}
