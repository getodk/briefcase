package org.opendatakit.briefcase.reused.cli;

import static java.util.Collections.emptySet;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class represents a Briefcase operation to be executed in a command-line environment
 * <p>
 * Uses a {@link Consumer}&lt;{@link Args}&gt; to pass command-line arguments to the logic of this {@link Operation}
 */
public class Operation {
  final Param param;
  final Consumer<Args> argsConsumer;
  final Set<Param> requiredParams;
  final Set<Param> optionalParams;
  Optional<Consumer<Args>> beforeCallback = Optional.empty();
  private final boolean deprecated;

  private Operation(Param param, Consumer<Args> argsConsumer, Set<Param> requiredParams, Set<Param> optionalParams, boolean deprecated) {
    this.param = param;
    this.argsConsumer = argsConsumer;
    this.requiredParams = requiredParams;
    this.optionalParams = optionalParams;
    this.deprecated = deprecated;
  }

  /**
   * Creates a new {@link Operation} without params of any kind (required or optional)
   *
   * @param param        main {@link Param} that will trigger the execution of this {@link Operation}, normally a {@link Param#flag(String, String, String)}
   * @param argsConsumer {@link Consumer}&lt;{@link Args}&gt; with the logic of this {@link Operation}
   * @return a new {@link Operation} instance
   */
  public static Operation of(Param param, Consumer<Args> argsConsumer) {
    return new Operation(param, argsConsumer, emptySet(), emptySet(), false);
  }

  /**
   * Creates a new {@link Operation} with some required params
   *
   * @param param          main {@link Param} that will trigger the execution of this {@link Operation}, normally a {@link Param#flag(String, String, String)}
   * @param argsConsumer   {@link Consumer}&lt;{@link Args}&gt; with the logic of this {@link Operation}
   * @param requiredParams a {@link Set}&lt;{@link Param}&gt; with the required params for this operation
   * @return a new {@link Operation} instance
   */
  public static Operation of(Param param, Consumer<Args> argsConsumer, List<Param> requiredParams) {
    return new Operation(param, argsConsumer, new HashSet<>(requiredParams), emptySet(), false);
  }

  /**
   * Creates a new {@link Operation} with some required and optional params
   *
   * @param param          main {@link Param} that will trigger the execution of this {@link Operation}, normally a {@link Param#flag(String, String, String)}
   * @param argsConsumer   {@link Consumer}&lt;{@link Args}&gt; with the logic of this {@link Operation}
   * @param requiredParams a {@link Set}&lt;{@link Param}&gt; with the required params for this operation
   * @param optionalParams a {@link Set}&lt;{@link Param}&gt; with the optional params for this operation
   * @return a new {@link Operation} instance
   */
  public static Operation of(Param param, Consumer<Args> argsConsumer, List<Param> requiredParams, List<Param> optionalParams) {
    return new Operation(param, argsConsumer, new HashSet<>(requiredParams), new HashSet<>(optionalParams), false);
  }

  /**
   * Creates a new deprecated {@link Operation} without params of any kind (required or optional)
   *
   * @param param        main {@link Param} that will trigger the execution of this {@link Operation}, normally a {@link Param#flag(String, String, String)}
   * @param argsConsumer {@link Consumer}&lt;{@link Args}&gt; with the logic of this {@link Operation}
   * @return a new {@link Operation} instance
   */
  static Operation deprecated(Param param, Consumer<Args> argsConsumer) {
    return new Operation(param, argsConsumer, emptySet(), emptySet(), true);
  }

  Set<Param> getAllParams() {
    // We need this because java.util.xyz collections are mutable
    HashSet<Param> allParams = new HashSet<>();
    allParams.add(param);
    allParams.addAll(requiredParams);
    allParams.addAll(optionalParams);
    return allParams;
  }

  boolean hasAnyParam() {
    return hasRequiredParams() || hasOptionalParams();
  }

  boolean hasOptionalParams() {
    return !optionalParams.isEmpty();
  }

  boolean hasRequiredParams() {
    return !requiredParams.isEmpty();
  }

  boolean isDeprecated() {
    return deprecated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Operation operation = (Operation) o;
    return Objects.equals(param, operation.param) &&
        Objects.equals(argsConsumer, operation.argsConsumer) &&
        Objects.equals(requiredParams, operation.requiredParams) &&
        Objects.equals(optionalParams, operation.optionalParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(param, argsConsumer, requiredParams, optionalParams);
  }

  @Override
  public String toString() {
    return "Operation{" +
        "param=" + param +
        ", argsConsumer=" + argsConsumer +
        ", requiredParams=" + requiredParams +
        ", optionalParams=" + optionalParams +
        '}';
  }

  public Operation before(Consumer<Args> callback) {
    beforeCallback = Optional.of(callback);
    return this;
  }
}
