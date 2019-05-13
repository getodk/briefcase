package org.opendatakit.briefcase.reused.job;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobsRunnerTest {
  private static final Logger log = LoggerFactory.getLogger(JobsRunnerTest.class);
  // We can't declare these variables locally because the compiler
  // complains about them not being effectively final. Declaring
  // them as fields somehow works around it
  private JobsRunner runner;
  private AtomicInteger externalOutput;

  @Before
  public void setUp() {
    runner = null;
    externalOutput = new AtomicInteger(0);
  }

  @After
  public void tearDown() {
    if (runner != null)
      runner.cancel();
  }

  @Test
  public void can_launch_a_job_asynchronously_and_cancel_it() {
    int expectedOutput = 1;
    runner = JobsRunner.launchAsync(
        Job.supply(returnWhenCancelled(expectedOutput)),
        result -> externalOutput.accumulateAndGet(result, Integer::sum),
        error -> log.error("Error in job", error)
    );
    // Give a chance to the background thread to launch the job and give us the runner
    sleep(100);
    runner.cancel();
    // Give a chance to the success callback to update our test state
    sleep(100);
    assertThat(externalOutput.get(), is(expectedOutput));
  }

  @Test
  public void can_launch_jobs_asynchronously_and_cancel_them() {
    runner = JobsRunner.launchAsync(
        IntStream.range(0, 100).mapToObj(n -> Job.supply(returnWhenCancelled(n))),
        result -> externalOutput.accumulateAndGet(result, Integer::sum),
        error -> log.error("Error in job", error)
    );
    // Give a chance to the background thread to launch the job and give us the runner
    sleep(100);
    runner.cancel();
    // Give a chance to the success callback to update our test state
    sleep(100);
    assertThat(externalOutput.get(), greaterThan(0));
  }

  @Test
  public void launched_async_jobs_will_eventually_end() {
    runner = JobsRunner.launchAsync(
        // Ensure that we will launch more Jobs than the thread pool's capacity
        IntStream.range(0, 1000).mapToObj(n -> Job.supply(__ -> 1)),
        result -> externalOutput.accumulateAndGet(result, Integer::sum),
        error -> log.error("Error in job", error)
    );
    // Give a chance to the jobs to complete
    sleep(100);
    assertThat(externalOutput.get(), is(1000));
  }

  @Test
  public void can_launch_a_job_synchronously() {
    JobsRunner.launchSync(
        Job.supply(__ -> 1),
        result -> externalOutput.accumulateAndGet(result, Integer::sum),
        error -> log.error("Error in job", error)
    );
    assertThat(externalOutput.get(), is(1));
    // TODO This should be:
    // assertThat(JobsRunner.launchSync(Job.supply(__ -> 1)), is(1));
  }

  @Test
  public void can_launch_jobs_synchronously() {
    JobsRunner.launchSync(
        IntStream.range(0, 1000).mapToObj(n -> Job.supply(__ -> 1)),
        result -> externalOutput.set(result.stream().mapToInt(i->i).sum()),
        error -> log.error("Error in job", error)
    );
    assertThat(externalOutput.get(), is(1000));
    // TODO This should be:
    // List<Integer> result = JobsRunner.launchSync(IntStream.range(0, 1000).mapToObj(n -> Job.supply(__ -> 1)));
    // assertThat(result, hasSize(1000));
    // assertThat(result.stream().mapToInt(i -> i).sum(), is(1000));
  }

  private <T> Function<RunnerStatus, T> returnWhenCancelled(T t) {
    return runnerStatus -> {
      while (runnerStatus.isStillRunning()) { }
      return t;
    };
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
