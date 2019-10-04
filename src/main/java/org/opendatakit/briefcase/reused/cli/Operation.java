package org.opendatakit.briefcase.reused.cli;

import java.util.HashSet;
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
  public final DeliveryType deliveryType;
  final Param param;
  final Consumer<Args> argsConsumer;
  final Set<Param> requiredParams;
  final Set<Param> optionalParams;
  final boolean deprecated;
  final Optional<Consumer<Args>> beforeCallback;

  Operation(DeliveryType deliveryType, Param param, Consumer<Args> argsConsumer, Set<Param> requiredParams, Set<Param> optionalParams, boolean deprecated, Optional<Consumer<Args>> beforeCallback) {
    this.deliveryType = deliveryType;
    this.param = param;
    this.argsConsumer = argsConsumer;
    this.requiredParams = requiredParams;
    this.optionalParams = optionalParams;
    this.deprecated = deprecated;
    this.beforeCallback = beforeCallback;
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
}
