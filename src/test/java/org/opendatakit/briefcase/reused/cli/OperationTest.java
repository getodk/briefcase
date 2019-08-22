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

package org.opendatakit.briefcase.reused.cli;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.Test;

public class OperationTest {


  private static final Consumer<Args> NO_OP = args -> {
  };


  @Test
  public void knows_if_it_has_required_params() {
    assertThat(buildOp(2, 0).hasRequiredParams(), is(true));
    assertThat(buildOp(2, 3).hasRequiredParams(), is(true));
    assertThat(buildOp(0, 0).hasRequiredParams(), is(false));
    assertThat(buildOp(0, 3).hasRequiredParams(), is(false));
  }

  @Test
  public void knows_if_it_has_optional_params() {
    assertThat(buildOp(2, 0).hasOptionalParams(), is(false));
    assertThat(buildOp(2, 3).hasOptionalParams(), is(true));
    assertThat(buildOp(0, 0).hasOptionalParams(), is(false));
    assertThat(buildOp(0, 3).hasOptionalParams(), is(true));
  }

  @Test
  public void knows_if_it_has_any_param() {
    assertThat(buildOp(2, 0).hasAnyParam(), is(true));
    assertThat(buildOp(2, 3).hasAnyParam(), is(true));
    assertThat(buildOp(0, 0).hasAnyParam(), is(false));
    assertThat(buildOp(0, 3).hasAnyParam(), is(true));
  }

  @Test
  public void knows_if_it_is_deprecated() {
    assertThat(buildDeprecatedOp().isDeprecated(), is(true));
    assertThat(buildOp(0, 0).isDeprecated(), is(false));
  }

  @Test
  public void returns_all_combined_params() {
    assertThat(buildOp(2, 2).getAllParams(), hasSize(5));
  }

  private static Operation buildOp(int requiredParams, int optionalParams) {
    return Operation.of(
        Param.flag("op", "operation", "Some operation"),
        NO_OP,
        IntStream.range(0, requiredParams).mapToObj(n -> Param.flag("r" + n, "required-" + n, "Required param " + n)).collect(toList()),
        IntStream.range(0, optionalParams).mapToObj(n -> Param.flag("o" + n, "optional-" + n, "Optional param " + n)).collect(toList())
    );
  }

  private static Operation buildDeprecatedOp() {
    return Operation.deprecated(Param.flag("op", "operation", "Some operation"), NO_OP);
  }
}
