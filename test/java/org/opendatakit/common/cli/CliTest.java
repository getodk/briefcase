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

package org.opendatakit.common.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.hamcrest.Matchers;
import org.junit.Test;

public class CliTest {

  @Test
  public void the_help_flag_prints_defined_ops() {
    Output output = captureOutputOf(() -> new Cli()
        .register(Operation.of(Param.flag("o", "operation", "Run some operation"), args -> {
        }))
        .run(new String[]{"-h"})
    );

    assertThat(output.err, Matchers.isEmptyString());
    assertThat(output.std, Matchers.containsString("-o"));
    assertThat(output.std, Matchers.containsString("--operation"));
    assertThat(output.std, Matchers.containsString("Run some operation"));
  }

  private Output captureOutputOf(Runnable block) {
    PrintStream backupOut = System.out;
    PrintStream backupErr = System.err;

    ByteArrayOutputStream stdStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errStream = new ByteArrayOutputStream();

    System.setOut(new PrintStream(stdStream));
    System.setErr(new PrintStream(errStream));

    block.run();

    System.setOut(backupOut);
    System.setErr(backupErr);

    return new Output(
        new String(stdStream.toByteArray(), UTF_8),
        new String(errStream.toByteArray(), UTF_8)
    );
  }

  class Output {
    final String std;
    final String err;

    Output(String std, String err) {
      this.std = std;
      this.err = err;
    }
  }
}