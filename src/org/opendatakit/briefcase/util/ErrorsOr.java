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
package org.opendatakit.briefcase.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO Replace this with an Either<List<String>, T>
public class ErrorsOr<T> {
  private final Optional<T> t;
  private final List<String> errors;

  public ErrorsOr(Optional<T> t, List<String> errors) {
    this.t = t;
    this.errors = errors;
  }

  public static <U> ErrorsOr<U> some(U u) {
    return new ErrorsOr<>(Optional.of(u), Collections.emptyList());
  }

  public static <U> ErrorsOr<U> errors(String... errors) {
    return new ErrorsOr<>(Optional.empty(), Arrays.asList(errors));
  }

  public List<String> getErrors() {
    return errors;
  }

  public T get() {
    return t.get();
  }
}
