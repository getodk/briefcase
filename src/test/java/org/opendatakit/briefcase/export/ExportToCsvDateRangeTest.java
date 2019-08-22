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

package org.opendatakit.briefcase.export;

import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvDateRangeTest {
  private ExportToCsvScenario scenario;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("simple-form");
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_submissions_inside_given_date_range() {
    String submissionTpl = scenario.readFile("simple-form-submission.xml.tpl");
    LocalDate startOfYear = LocalDate.of(2018, 1, 1);
    LocalDate startDate = LocalDate.of(2018, 4, 1);
    LocalDate endDate = LocalDate.of(2018, 5, 1);
    IntStream.range(0, 365).boxed().forEach(n -> scenario.createInstance(submissionTpl, startOfYear.plusDays(n)));
    scenario.runExport(startDate, endDate);
    scenario.assertSameContent();
  }
}