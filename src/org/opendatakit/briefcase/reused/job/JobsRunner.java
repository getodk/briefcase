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

import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.opendatakit.briefcase.reused.job.CompletableFutureHelpers.collectResult;

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
 * Runs {@link Job} instances in synchronous or asyncjhronous modes.
 */
public class JobsRunner {
  private static final Logger log = LoggerFactory.getLogger(JobsRunner.class);
  private static final ForkJoinPool SYSTEM_FORK_JOIN_POOL = commonPool();
  private final ExecutorService executor;

  private JobsRunner(ForkJoinPool executor) {
    this.executor = executor;
  }

  /**
   * Run the provided stream of jobs asynchronously and return an
   * instance that will let the caller cancel the background process.
   */
  public static <T> JobsRunner launchAsync(Stream<Job<T>> jobs, Consumer<List<T>> onSuccess, Consumer<Throwable> onError) {
    // Get a clone of the ForkJoinPool that we can shutdown
    ForkJoinPool executor = buildCancellableForkJoinPool(onError);
    // Fork off a thread to act as the main thread of this collection of background jobs
    CompletableFuture.runAsync(() -> {
      try {
        onSuccess.accept(jobs.map(job -> job.launch(executor)).collect(collectResult()).get());
        executor.shutdown();
      } catch (InterruptedException | ExecutionException e) {
        log.warn("Job cancelled", e);
        onError.accept(e);
      }
    }, executor);
    // Return a JobsRunner to let the calling site cancel this background operation
    return new JobsRunner(executor);
  }

  /**
   * Run the provided job asynchronously and return an instance that
   * will let the caller cancel the background process.
   */
  public static <T> JobsRunner launchAsync(Job<T> job, Consumer<T> onSuccess, Consumer<Throwable> onError) {
    ForkJoinPool executor = commonPool();
    // Fork off a thread to act as the main thread of this collection of background jobs
    CompletableFuture.runAsync(() -> {
      try {
        onSuccess.accept(job.launch(executor).get());
      } catch (InterruptedException | ExecutionException e) {
        log.warn("Job cancelled", e);
        onError.accept(e);
      }
    });
    return new JobsRunner(executor);
  }

  /**
   * Run the provided stream of jobs synchronously (blocking the current Thread).
   */
  // TODO v2.0 make it return List<T>, since the JobsRunner can't really be used for anything
  public static <T> JobsRunner launchSync(Stream<Job<T>> jobs, Consumer<List<T>> onSuccess, Consumer<Throwable> onError) {
    ForkJoinPool executor = commonPool();
    // Canonical use of the ForkJoinPool and Completable futures
    try {
      onSuccess.accept(jobs
          .map(job -> job.launch(executor)).collect(collectResult())
          // Wait until the CompletableFutures are completed
          .get());
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error launching sync jobs", e);
      onError.accept(e);
    }
    return new JobsRunner(executor);
  }

  // TODO v2.0 make it return T, since the JobsRunner can't really be used for anything
  public static <T> JobsRunner launchSync(Job<T> job, Consumer<T> onSuccess, Consumer<Throwable> onError) {
    ForkJoinPool executor = commonPool();
    // Canonical use of the ForkJoinPool and Completable futures
    try {
      onSuccess.accept(job
          .launch(executor)
          // Wait until the CompletableFuture is completed
          .get());
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error launching sync jobs", e);
      onError.accept(e);
    }
    return new JobsRunner(executor);
  }

  private static ForkJoinPool buildCancellableForkJoinPool(Consumer<Throwable> onError) {
    return new ForkJoinPool(
        SYSTEM_FORK_JOIN_POOL.getParallelism(),
        SYSTEM_FORK_JOIN_POOL.getFactory(),
        (thread, throwable) -> onError.accept(throwable),
        SYSTEM_FORK_JOIN_POOL.getAsyncMode()
    );
  }

  /**
   * Cancels the process managed by this JobsRunner instance.
   * No new tasks will be accepted and any enqueued task will be ignored.
   * Ongoing tasks will complete.
   */
  public void cancel() {
    executor.shutdownNow();
  }
}
