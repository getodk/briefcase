package org.opendatakit.common.cli;

import org.apache.commons.cli.Option;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class Param<T> {
  final String shortCode;
  final Option option;
  private final Optional<Function<String, T>> mapper;

  private Param(String shortCode, Option option, Optional<Function<String, T>> mapper) {
    this.shortCode = shortCode;
    this.option = option;
    this.mapper = mapper;
  }

  public static Param<String> arg(String shortCode, String longCode, String description) {
    return new Param<>(
        shortCode,
        new Option(shortCode, longCode, true, description),
        Optional.of(Function.identity())
    );
  }

  public static <U> Param<U> arg(String shortCode, String longCode, String description, Function<String, U> mapper) {
    return new Param<>(
        shortCode,
        new Option(shortCode, longCode, true, description),
        Optional.of(mapper)
    );
  }

  public static Param<Void> flag(String shortCode, String longCode, String description) {
    return new Param<>(
        shortCode,
        new Option(shortCode, longCode, false, description),
        Optional.empty()
    );
  }

  boolean isArg() {
    return this.option.hasArg();
  }

  public T map(String value) {
    return this.mapper
        .orElseThrow(() -> new RuntimeException("No mapper defined for param -" + shortCode))
        .apply(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Param that = (Param) o;
    return Objects.equals(shortCode, that.shortCode) &&
        Objects.equals(option, that.option);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortCode, option);
  }

  @Override
  public String toString() {
    return option.toString();
  }
}
