package org.opendatakit.common.cli;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.cli.Option;

/**
 * This class represents a command-line execution argument. It can be either a flag (without a
 * value associated to it) or an arg (with some value).
 * <p>
 * All params are of type {@link String} by default and they can have a {@link Function}&lt;{@link String}, T&gt; mapper
 * that can produce values of other types if needed.
 *
 * @param <T> the type of the value it holds. {@link String} by default or {@link Void} for flags
 */
public class Param<T> {
  final String shortCode;
  final Option option;
  private final Optional<Function<String, T>> mapper;

  private Param(String shortCode, Option option, Optional<Function<String, T>> mapper) {
    this.shortCode = shortCode;
    this.option = option;
    this.mapper = mapper;
  }

  /**
   * Creates a new {@link Param}&lt;{@link String}&gt; instance for a command-line arg
   *
   * @param shortCode   the shortcode (usually one or two chars)
   * @param longCode    the longcode (usually some words separated by hyphens)
   * @param description the description
   * @return a new {@link Param}&lt;{@link String}&gt; instance
   */
  public static Param<String> arg(String shortCode, String longCode, String description) {
    return new Param<>(
        shortCode,
        new Option(shortCode, longCode, true, description),
        Optional.of(Function.identity())
    );
  }

  /**
   * Creates a new {@link Param}&lt;U&gt; instance for a command-line arg mapping the received {@link String} value
   *
   * @param shortCode   the shortcode (usually one or two chars)
   * @param longCode    the longcode (usually some words separated by hyphens)
   * @param description the description
   * @param mapper      a mapper {@link Function}&lt;{@link String}, U&gt; function that will transform the received {@link String} into a value of type <em>U</em>
   * @param <U>         the type of the resulting value after transforming it with <em>mapper</em>
   * @return a new {@link Param}&lt;U&gt; instance
   */
  public static <U> Param<U> arg(String shortCode, String longCode, String description, Function<String, U> mapper) {
    return new Param<>(
        shortCode,
        new Option(shortCode, longCode, true, description),
        Optional.of(mapper)
    );
  }

  /**
   * Creates a new {@link Param}&lt;{@link Void}&gt; instance for a command-line flag
   *
   * @param shortCode   the shortcode (usually one or two chars)
   * @param longCode    the longcode (usually some words separated by hyphens)
   * @param description the description
   * @return a new {@link Param}&lt;{@link Void}&gt; instance
   */
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

  boolean isFlag() {
    return !this.option.hasArg();
  }

  T map(String value) {
    return this.mapper
        .orElseThrow(() -> new RuntimeException("No mapper defined for param -" + shortCode))
        .apply(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Param<?> param = (Param<?>) o;
    return Objects.equals(shortCode, param.shortCode) &&
        Objects.equals(option, param.option) &&
        Objects.equals(mapper, param.mapper);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortCode, option, mapper);
  }

  @Override
  public String toString() {
    return "Param{" +
        "shortCode='" + shortCode + '\'' +
        ", option=" + option +
        ", mapper=" + mapper +
        '}';
  }
}
