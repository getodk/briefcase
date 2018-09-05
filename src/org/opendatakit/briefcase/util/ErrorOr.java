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

import java.util.Optional;
import java.util.function.Consumer;

// TODO Replace this with an Either<List<String>, T>
public class ErrorOr<T> {
  private final Optional<T> value;
  private final Optional<String> error;

  private ErrorOr(Optional<T> value, Optional<String> error) {
    this.value = value;
    this.error = error;
  }

  public static <U> ErrorOr<U> some(U value) {
    return new ErrorOr<>(Optional.of(value), Optional.empty());
  }

  public static <U> ErrorOr<U> error(String error) {
    return new ErrorOr<>(Optional.empty(), Optional.of(error));
  }

  public static <U> ErrorOr<U> from(Optional<U> maybeValue, String errorIfEmpty) {
    return maybeValue
        .map(ErrorOr::some)
        .orElseGet(() -> error(errorIfEmpty));
  }

  public void ifError(Consumer<String> consumer) {
    error.ifPresent(consumer);
  }

  public Optional<T> asOptional() {
    return value;
  }
}
