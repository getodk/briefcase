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
package org.opendatakit.briefcase.reused.api;

import java.util.Arrays;
import java.util.Optional;

public class Optionals {
  /**
   * Returns the first (the winner of the "race") {@link Optional} in the array that is present.
   *
   * @param optionals an array of {@link Optional} instances to be evalued
   * @return the first {@link Optional} in the array that is present
   */
  @SafeVarargs
  public static <T> Optional<T> race(Optional<T>... optionals) {
    return Arrays.stream(optionals)
        .filter(Optional::isPresent)
        .findFirst()
        .flatMap(o -> o);
  }
}
