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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class ExportToCsvTest {
  private final String suffix;
  private final ExportToCsvScenario scenario;

  public ExportToCsvTest(String suffix, String testCase, String value) {
    scenario = ExportToCsvScenario.setUp("simple-form");
    String tpl = scenario.readFile("simple-form-submission.xml.tpl");
    scenario.createInstance(tpl, LocalDate.of(2018, 1, 1), value);
    this.suffix = suffix;
  }

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"empty", "empty field", ""},
        {"simple", "simplest of forms", "some text"},
        {"newlines", "encodes strings with newlines", "some \n text"},
        {"commas", "encodes strings with commas", "some , text"},
        {"double-quotes", "encodes strings with double quotes", "some \" text"},
    });
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_to_csv() {
    scenario.runExport();
    scenario.assertSameContent(suffix);
  }
}
