package org.opendatakit.common.cli;

import static java.util.Collections.emptySet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Operation {
  final Param param;
  final Consumer<Args> argsConsumer;
  final Set<Param> requiredParams;
  final Set<Param> optionalParams;

  private Operation(Param param, Consumer<Args> argsConsumer, Set<Param> requiredParams, Set<Param> optionalParams) {
    this.param = param;
    this.argsConsumer = argsConsumer;
    this.requiredParams = requiredParams;
    this.optionalParams = optionalParams;
  }

  public static Operation of(Param param, Consumer<Args> argsConsumer) {
    return new Operation(param, argsConsumer, emptySet(), emptySet());
  }

  public static Operation of(Consumer<Args> argsConsumer, Set<Param> requiredParams) {
    return new Operation(null, argsConsumer, requiredParams, emptySet());
  }

  public static Operation of(Param param, Consumer<Args> argsConsumer, List<Param> requiredParams) {
    return new Operation(param, argsConsumer, new HashSet<>(requiredParams), emptySet());
  }

  public static Operation of(Param param, Consumer<Args> argsConsumer, List<Param> requiredParams, List<Param> optionalParams) {
    return new Operation(param, argsConsumer, new HashSet<>(requiredParams), new HashSet<>(optionalParams));
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

}
