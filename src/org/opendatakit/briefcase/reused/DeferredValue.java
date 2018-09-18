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

package org.opendatakit.briefcase.reused;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simplified API to handle {@link CompletableFuture} asynchronous values.
 * <p>
 * This class allows running code in a background thread using Java's Fork/Join pool.
 */
public class DeferredValue<T> {
  private final CompletableFuture<T> future;

  private DeferredValue(CompletableFuture<T> future) {
    this.future = future;
  }

  /**
   * Returns a deferred value using the supplier to obtain it.
   *
   * @see DeferredValue#thenApply(Function)
   * @see DeferredValue#thenAccept(Consumer)
   * @see DeferredValue#onError(Consumer)
   */
  public static <U> DeferredValue<U> of(Supplier<U> valueSupplier) {
    return new DeferredValue<>(CompletableFuture.supplyAsync(valueSupplier));
  }

  /**
   * Returns a new deferred value after mapping any value obtained at this stage.
   */
  public <U> DeferredValue<U> thenApply(Function<T, U> valueMapper) {
    return new DeferredValue<>(future.thenApplyAsync(valueMapper));
  }

  /**
   * Returns a new deferred value after consuming any value obtained at this stage.
   */
  public DeferredValue<T> thenAccept(Consumer<T> valueConsumer) {
    return new DeferredValue<>(future.thenApplyAsync(value -> {
      valueConsumer.accept(value);
      return value;
    }));
  }

  /**
   * Returns a new deferred value after consuming any error produced at this stage.
   */
  public DeferredValue<T> onError(Consumer<Throwable> errorConsumer) {
    return new DeferredValue<>(future.exceptionally(error -> {
      errorConsumer.accept(error);
      return null;
    }));
  }
}
