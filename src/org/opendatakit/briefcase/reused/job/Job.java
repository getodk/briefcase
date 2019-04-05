/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.reused.job;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.Triple;

/**
 * A job models some computation that might return a value. Jobs
 * are aware of the runnerStatus where they are launched in order to be
 * able to check its premature stop, which lets calling sites
 * effectively cancel ongoing background processes.
 * <p>
 * Jobs that don't return a value are of type Void.
 */
public class Job<T> {
  private final Function<RunnerStatus, T> runnerAwareSupplier;

  private Job(Function<RunnerStatus, T> runnerAwareSupplier) {
    this.runnerAwareSupplier = runnerAwareSupplier;
  }

  /**
   * Return a new job that will return a value of type U once launched.
   */
  public static <U> Job<U> supply(Function<RunnerStatus, U> runnerAwareSupplier) {
    return new Job<>(runnerAwareSupplier);
  }

  /**
   * Return a new job that won't return any value once launched.
   */
  public static Job<Void> run(Consumer<RunnerStatus> runnerAwareRunnable) {
    return new Job<>(runnerStatus -> {
      runnerAwareRunnable.accept(runnerStatus);
      return null;
    });
  }

  public static final Job<Void> noOp = Job.run(__ -> {});

  public static <T> Job<T> noOpSupplier() {
    return Job.supply(__ -> null);
  }

  /**
   * Composes three jobs into one that will return a {@link Triple}
   * with all their individual values once launched.
   */
  public static <T, U, V> Job<Triple<T, U, V>> allOf(Job<T> t, Job<U> u, Job<V> v) {
    return new Job<>(runnerStatus -> new Triple<>(
        t.runnerAwareSupplier.apply(runnerStatus),
        u.runnerAwareSupplier.apply(runnerStatus),
        v.runnerAwareSupplier.apply(runnerStatus)
    ));
  }

  /**
   * Composes this job with another job that will receive this one's value once launched.
   */
  public <U> Job<U> thenApply(BiFunction<RunnerStatus, T, U> runnerAwareFunction) {
    return new Job<>(runnerStatus -> runnerAwareFunction.apply(runnerStatus, runnerAwareSupplier.apply(runnerStatus)));
  }

  public <U> Job<U> thenRun(Job<U> job) {
    return new Job<>(runnerStatus -> {
      runnerAwareSupplier.apply(runnerStatus);
      return job.runnerAwareSupplier.apply(runnerStatus);
    });
  }

  CompletableFuture<T> launch(ExecutorService executor) {
    return CompletableFuture.supplyAsync(() -> runnerAwareSupplier.apply(new RunnerStatus(executor::isShutdown)), executor);
  }
}
