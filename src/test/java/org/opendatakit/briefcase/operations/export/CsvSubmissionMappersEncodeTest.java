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

package org.opendatakit.briefcase.operations.export;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class CsvSubmissionMappersEncodeTest {

  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public boolean allowNulls;

  @Parameterized.Parameter(value = 2)
  public String input;

  @Parameterized.Parameter(value = 3)
  public String expectedOutput;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"empty, no nulls allowed", false, "", "\"\""},
        {"empty, nulls allowed", true, "", ""},
        {"normal", false, "one two three", "one two three"},
        {"text with commas", true, "one, two, three", "\"one, two, three\""},
        {"text with double quotes", true, "one \"two\" three", "\"one \"\"two\"\" three\""},
        {"text with new line chars", true, "one\ntwo\nthree", "\"one\ntwo\nthree\""},
    });
  }

  @Test
  public void encode() {
    assertThat(CsvSubmissionMappers.encode(input, allowNulls), is(expectedOutput));
  }
}
