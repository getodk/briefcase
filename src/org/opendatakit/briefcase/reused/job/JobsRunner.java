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

import static org.opendatakit.briefcase.reused.job.CompletableFutureHelpers.collectResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a collection of {@link Job} jobs in a ForkJoinPool
 */
public class JobsRunner<T> {
  private static final Logger log = LoggerFactory.getLogger(JobsRunner.class);
  private final List<Consumer<List<T>>> successCallbacks = new ArrayList<>();
  private final List<Consumer<Throwable>> errorCallbacks = new ArrayList<>();
  private final ExecutorService executor;

  public JobsRunner() {
    executor = new ForkJoinPool(
        ForkJoinPool.commonPool().getParallelism(),
        ForkJoinPool.commonPool().getFactory(),
        (thread, throwable) -> errorCallbacks.forEach(c -> c.accept(throwable)),
        ForkJoinPool.commonPool().getAsyncMode()
    );
  }

  @SafeVarargs
  public static <U> JobsRunner<U> launchAsync(Job<U>... jobs) {
    return new JobsRunner<U>().launchAsync(Stream.of(jobs));
  }

  /**
   * Launches the jobs in background.
   */
  public JobsRunner<T> launchAsync(Stream<Job<T>> jobs) {
    launch(jobs, false);
    return this;
  }

  @SafeVarargs
  public static <U> JobsRunner<U> launchSync(Job<U>... jobs) {
    return new JobsRunner<U>().launchSync(Stream.of(jobs));
  }

  /**
   * Launches the jobs and blocks the current thread.
   */
  public JobsRunner<T> launchSync(Stream<Job<T>> jobs) {
    launch(jobs, true);
    return this;
  }

  private void launch(Stream<Job<T>> jobs, boolean join) {
    CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
      try {
        List<T> results = jobs.map(job -> job.launch(executor)).collect(collectResult()).get();
        successCallbacks.forEach(c -> c.accept(results));
        executor.shutdown();
      } catch (InterruptedException | ExecutionException e) {
        log.info("Job cancelled", e);
      }
    }, executor);
    if (join)
      completableFuture.join();
  }

  /**
   * Lets calling site perform side-effects when an uncaught exception reaches the runner
   */
  public JobsRunner<T> onError(Consumer<Throwable> errorCallback) {
    errorCallbacks.add(errorCallback);
    return this;
  }

  /**
   * Lets calling site perform side-effects after successfully completing all its jobs.
   */
  public JobsRunner<T> onSuccess(Consumer<List<T>> successCallback) {
    successCallbacks.add(successCallback);
    return this;
  }

  /**
   * Cancels the background jobs:
   * <ul>
   * <li>Ongoing jobs won't be affected</li>
   * <li>Jobs in the queue won't be started</li>
   * <li>New jobs will be rejected</li>
   * </ul>
   */
  public void cancel() {
    executor.shutdownNow();
  }
}
