package org.opendatakit.briefcase.reused.job;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Function;
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
  private int externalOutput;

  @Before
  public void setUp() {
    runner = null;
    externalOutput = 0;
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
        result -> externalOutput = result,
        error -> log.error("Error in job", error)
    );
    // Give a chance to the background thread to launch the job and give us the runner
    sleep(100);
    runner.cancel();
    // Give a chance to the success callback to update our test state
    sleep(100);
    assertThat(externalOutput, is(expectedOutput));
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
