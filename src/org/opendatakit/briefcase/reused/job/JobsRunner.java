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
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
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
  private final ForkJoinPool executor;
  private Optional<Runnable> onCompleteCallback = Optional.empty();

  private JobsRunner(ForkJoinPool executor) {
    this.executor = executor;
  }

  /**
   * Run the provided stream of jobs asynchronously and return an
   * instance that will let the caller cancel the background process.
   *
   * @see #launchAsync(Stream, Consumer)
   */
  public static JobsRunner launchAsync(Stream<Job<?>> jobs) {
    return launchAsync(jobs, __ -> { });
  }

  /**
   * Run the provided stream of jobs asynchronously and return an
   * instance that will let the caller cancel the background process.
   *
   * @param onError callback that will be executed each time an individual job throws an exception
   */
  public static JobsRunner launchAsync(Stream<Job<?>> jobs, Consumer<Throwable> onError) {
    ForkJoinPool executor = buildCancellableForkJoinPool();
    RunnerStatus runnerStatus = new RunnerStatus(executor::isShutdown);
    jobs.forEach(job -> executor.submit(() -> {
      try {
        job.runnerAwareSupplier.apply(runnerStatus);
      } catch (Throwable t) {
        log.error("Error running Job", t);
        onError.accept(t);
      }
    }));
    return new JobsRunner(executor);
  }

  /**
   * Run the provided job asynchronously and return an instance that
   * will let the caller cancel the background process.
   *
   * @see #launchAsync(Job, Consumer)
   */
  public static JobsRunner launchAsync(Job<?> job) {
    return launchAsync(job, __ -> { });
  }

  /**
   * Run the provided job asynchronously and return an instance that
   * will let the caller cancel the background process.
   *
   * @param onError callback that will be executed when the provided job throws an exception
   */
  public static JobsRunner launchAsync(Job<?> job, Consumer<Throwable> onError) {
    return launchAsync(Stream.of(job), onError);
  }

  /**
   * Run the provided stream of jobs synchronously (blocking the current Thread)
   * and return a list with their outputs.
   */
  public static <T> List<T> launchSync(Stream<Job<T>> jobs) {
    RunnerStatus runnerStatus = new RunnerStatus(() -> false);
    return jobs.parallel()
        .map(job -> job.runnerAwareSupplier.apply(runnerStatus))
        .collect(toList());
  }

  /**
   * Run the provided job synchronously (blocking the current Thread) and return
   * its output.
   */
  public static <T> T launchSync(Job<T> job) {
    return launchSync(Stream.of(job)).get(0);
  }

  private static ForkJoinPool buildCancellableForkJoinPool() {
    return new ForkJoinPool(
        SYSTEM_FORK_JOIN_POOL.getParallelism(),
        SYSTEM_FORK_JOIN_POOL.getFactory(),
        (thread, throwable) -> log.error("An uncaught exception reached the thread pool", throwable),
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

  /**
   * Blocks current thread until all the submitted jobs are completed (executor's
   * quiescent state), and shuts down the executor, preventing more work to be
   * submitted
   */
  public void waitForCompletion() {
    try {
      while (!executor.isQuiescent())
        Thread.sleep(10);
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
      onCompleteCallback.ifPresent(Runnable::run);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public JobsRunner onComplete(Runnable onCompleteCallback) {
    // TODO The callback will only be called when combined with waitForCompletion, which makes it really weird
    // TODO Since the design of JobsRunner isn't intended to accept more jobs after instantiating it, we might want to actually always waitForCompletion by default, maybe submitting an extra long-running job that will call the callback by default.
    this.onCompleteCallback = Optional.of(onCompleteCallback);
    return this;
  }
}
