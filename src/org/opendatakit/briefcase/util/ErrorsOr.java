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
