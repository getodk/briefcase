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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.Triple;

/**
 * A job models some computation that might return a value. Jobs
 * are aware of the runnerStatus where they are launched in order to be
 * able to check its premature stop, which lets calling sites
 * effectively cancel ongoing background processes.
 * <p>
 * Jobs that don't return a value are of type Void.
 */
// TODO v2.0 Study if using @FunctionalInterface could produce a cleaner API. The idea is to compose only Job instances, not Jobs with Functions
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

  /**
   * Convenience job that won't do any computation
   */
  public static final Job<Void> noOp = Job.run(__ -> { });

  /**
   * Convenience factory of Jobs that won't do any computation
   * and will return null in a type-safe way.
   */
  public static <T> Job<T> noOpSupplier() {
    return Job.supply(__ -> null);
  }

  /**
   * Composes two jobs into one that will return a {@link Pair}
   * with all their individual values once completed.
   */
  public static <T, U> Job<Pair<T, U>> allOf(Job<T> t, Job<U> u) {
    return new Job<>(runnerStatus -> Pair.of(
        t.runnerAwareSupplier.apply(runnerStatus),
        u.runnerAwareSupplier.apply(runnerStatus)
    ));
  }

  /**
   * Composes three jobs into one that will return a {@link Triple}
   * with all their individual values once completed.
   */
  public static <T, U, V> Job<Triple<T, U, V>> allOf(Job<T> t, Job<U> u, Job<V> v) {
    return new Job<>(runnerStatus -> new Triple<>(
        t.runnerAwareSupplier.apply(runnerStatus),
        u.runnerAwareSupplier.apply(runnerStatus),
        v.runnerAwareSupplier.apply(runnerStatus)
    ));
  }

  /**
   * Creates a new Job that will feed this job's result into the provided function
   * to return a value once completed.
   */
  public <U> Job<U> thenApply(BiFunction<RunnerStatus, T, U> runnerAwareFunction) {
    return new Job<>(runnerStatus -> runnerAwareFunction.apply(runnerStatus, runnerAwareSupplier.apply(runnerStatus)));
  }

  /**
   * Creates a new Job that will feed this job's result into the provided function
   * and return null once completed.
   */
  public <U> Job<U> thenRun(Consumer<RunnerStatus> runnerAwareConsumer) {
    return new Job<>(runnerStatus -> {
      runnerAwareSupplier.apply(runnerStatus);
      runnerAwareConsumer.accept(runnerStatus);
      return null;
    });
  }

  /**
   * Composes this job with the provided job so that it launches them in sequence
   * and returns the provided job's once completed.
   */
  // TODO v2.0 Rename to thenSupply, since this is returning the provided Job's output without consuming this one's output
  public <U> Job<U> thenRun(Job<U> job) {
    return new Job<>(runnerStatus -> {
      runnerAwareSupplier.apply(runnerStatus);
      return job.runnerAwareSupplier.apply(runnerStatus);
    });
  }

  /**
   * Creates a new Job that will feed this job's result into the provided function
   * and return null once completed.
   */
  public Job<Void> thenAccept(BiConsumer<RunnerStatus, T> runnerAwareConsumer) {
    return new Job<>(runnerStatus -> {
      T t = runnerAwareSupplier.apply(runnerStatus);
      runnerAwareConsumer.accept(runnerStatus, t);
      return null;
    });
  }

  /**
   * Creates a new Job that will launch this job and the provided function in sequence
   * and returns the provided function's result once completed.
   */
  public <U> Job<U> thenSupply(Function<RunnerStatus, U> runnerAwareSupplier) {
    return new Job<>(runnerStatus -> {
      this.runnerAwareSupplier.apply(runnerStatus);
      return runnerAwareSupplier.apply(runnerStatus);
    });
  }

  CompletableFuture<T> launch(ExecutorService executor) {
    return CompletableFuture.supplyAsync(() -> runnerAwareSupplier.apply(new RunnerStatus(executor::isShutdown)), executor);
  }
}
