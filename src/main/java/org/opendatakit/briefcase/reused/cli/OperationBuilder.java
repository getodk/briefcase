package org.opendatakit.briefcase.reused.cli;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class OperationBuilder {
  private Param flag;
  private Consumer<Args> argsConsumer;
  private Set<Param> requiredParams = new HashSet<>();
  private Set<Param> optionalParams = new HashSet<>();
  private boolean deprecated = false;
  private Optional<Consumer<Args>> beforeCallback = Optional.empty();

  public Operation build() {
    return new Operation(
        Objects.requireNonNull(flag),
        Objects.requireNonNull(argsConsumer),
        requiredParams,
        optionalParams,
        deprecated,
        beforeCallback
    );
  }

  public OperationBuilder withFlag(Param flag) {
    this.flag = flag;
    return this;
  }

  public OperationBuilder withRequiredParams(Param... param) {
    requiredParams.addAll(Arrays.asList(param));
    return this;
  }

  public OperationBuilder withOptionalParams(Param... param) {
    optionalParams.addAll(Arrays.asList(param));
    return this;
  }

  public OperationBuilder withLauncher(Consumer<Args> argsConsumer) {
    this.argsConsumer = argsConsumer;
    return this;
  }

  public OperationBuilder withBefore(Consumer<Args> beforeCallback) {
    this.beforeCallback = Optional.of(beforeCallback);
    return this;
  }

  public OperationBuilder deprecated() {
    deprecated = true;
    return this;
  }
}
