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
package org.opendatakit.briefcase.reused;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

public class Lists {
  /**
   * Creates a new {@link List} appending all the elements in the right {@link List} after
   * the last element in the left {@link List}.
   *
   * @return a new {@link List}
   */
  public static <T> List<T> concat(List<T> left, List<T> right) {
    return Stream.of(left, right)
        .flatMap(List::stream)
        .collect(toList());
  }

  /**
   * Creates a new {@link Stream} appending all the elements in the right {@link Stream} after
   * the last element in the left {@link Stream}.
   *
   * @return a new {@link Stream}
   */
  public static <T> Stream<T> concat(Stream<T> left, Stream<T> right) {
    return Stream.of(left, right).flatMap(i -> i);
  }

  /**
   * Creates a new {@link Stream} prepending a value before the first element on the given {@link Stream}.
   *
   * @return a new {@link Stream}
   */
  public static <T> Stream<T> prepend(T value, Stream<T> right) {
    return Stream.of(Stream.of(value), right).flatMap(i -> i);
  }
}
