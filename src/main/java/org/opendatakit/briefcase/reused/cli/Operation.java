package org.opendatakit.briefcase.reused.cli;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This class represents a Briefcase operation to be executed in a command-line environment
 * <p>
 * Uses a {@link Consumer}&lt;{@link Args}&gt; to pass command-line arguments to the logic of this {@link Operation}
 */
public class Operation {
  public final DeliveryType deliveryType;
  private final String name;
  private final Predicate<Args> matcher;
  final Consumer<Args> argsConsumer;
  final Set<Param> requiredParams;
  final Set<Param> optionalParams;
  final boolean deprecated;
  final Optional<Consumer<Args>> beforeCallback;
  final boolean requiresContainer;

  Operation(DeliveryType deliveryType, String name, Predicate<Args> matcher, Consumer<Args> argsConsumer, Set<Param> requiredParams, Set<Param> optionalParams, boolean deprecated, Optional<Consumer<Args>> beforeCallback, boolean requiresContainer) {
    this.deliveryType = deliveryType;
    this.name = name;
    this.matcher = matcher;
    this.argsConsumer = argsConsumer;
    this.requiredParams = requiredParams;
    this.optionalParams = optionalParams;
    this.deprecated = deprecated;
    this.beforeCallback = beforeCallback;
    this.requiresContainer = requiresContainer;
  }

  public String getName() {
    return name;
  }

  Set<Param> getAllParams() {
    // We need this because java.util.xyz collections are mutable
    HashSet<Param> allParams = new HashSet<>();
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

  public boolean matches(Args args) {
    return matcher.test(args);
  }

  public Set<Param> getRequiredParams() {
    return requiredParams;
  }

  public Set<Param> getOptionalParams() {
    return optionalParams;
  }

  public boolean requiresContainer() {
    return requiresContainer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Operation operation = (Operation) o;
    return deprecated == operation.deprecated &&
        Objects.equals(deliveryType, operation.deliveryType) &&
        Objects.equals(matcher, operation.matcher) &&
        Objects.equals(argsConsumer, operation.argsConsumer) &&
        Objects.equals(requiredParams, operation.requiredParams) &&
        Objects.equals(optionalParams, operation.optionalParams) &&
        Objects.equals(beforeCallback, operation.beforeCallback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deliveryType, matcher, argsConsumer, requiredParams, optionalParams, deprecated, beforeCallback);
  }
}
