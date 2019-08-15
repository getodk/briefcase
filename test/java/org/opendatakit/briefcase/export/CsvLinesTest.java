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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class CsvLinesTest {

  /**
   * Create a line on a date with the date as the only content
   */
  private static CsvLine createCsvLine(String someInstanceId, String someDateTime) {
    return new CsvLine(someInstanceId, OffsetDateTime.parse(someDateTime), someDateTime);
  }

  @Test
  public void lines_can_be_obtained_sorted_by_insertion_order() {
    CsvLines csvLines = new CsvLines("some fqdn", Arrays.asList(
        createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z"),
        createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z")
    ));
    List<String> lines = csvLines.unsorted().collect(toList());
    assertThat(lines, hasSize(2));
    assertThat(lines, contains("2018-01-02T00:00:00.000Z", "2018-01-01T00:00:00.000Z"));
  }

  @Test
  public void lines_can_be_obtained_sorted_by_submission_date() {
    CsvLines csvLines = new CsvLines("some fqdn", Arrays.asList(
        createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z"),
        createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z")
    ));
    List<String> lines = csvLines.sorted().collect(toList());
    assertThat(lines, hasSize(2));
    assertThat(lines, contains("2018-01-01T00:00:00.000Z", "2018-01-02T00:00:00.000Z"));
  }

  @Test
  public void instances_with_the_same_fqdn_can_be_merged() {
    CsvLines csvLines1 = new CsvLines("some fqdn", singletonList(createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z")));
    CsvLines csvLines2 = new CsvLines("some fqdn", singletonList(createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z")));
    CsvLines mergedCsvLines = CsvLines.merge(csvLines1, csvLines2);
    assertThat(mergedCsvLines.getModelFqn(), is("some fqdn"));
    List<String> lines = mergedCsvLines.unsorted().collect(toList());
    // We don't care about the order but we need to know that all the expected lines are there
    assertThat(lines, hasSize(2));
    assertThat(lines, containsInAnyOrder("2018-01-01T00:00:00.000Z", "2018-01-02T00:00:00.000Z"));
  }

  @Test
  public void instances_can_be_merged_even_if_one_does_not_have_fqdn() {
    CsvLines empty = CsvLines.empty();
    CsvLines nonEmpty = new CsvLines("some fqdn", singletonList(createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z")));

    CsvLines leftWasEmpty = CsvLines.merge(empty, nonEmpty);
    assertThat(leftWasEmpty.getModelFqn(), is("some fqdn"));
    assertThat(leftWasEmpty.unsorted().collect(toList()), hasSize(1));

    CsvLines rightWasEmpty = CsvLines.merge(nonEmpty, empty);
    assertThat(rightWasEmpty.getModelFqn(), is("some fqdn"));
    assertThat(rightWasEmpty.unsorted().collect(toList()), hasSize(1));
  }

  @Test(expected = BriefcaseException.class)
  public void throws_when_merging_two_instances_with_non_matching_fqdns() {
    CsvLines csvLines1 = new CsvLines("some fqdn", singletonList(createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z")));
    CsvLines csvLines2 = new CsvLines("some other fqdn", singletonList(createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z")));
    CsvLines.merge(csvLines1, csvLines2);
  }

  @Test(expected = BriefcaseException.class)
  public void throws_when_merging_two_instances_without_fqdns() {
    CsvLines.merge(CsvLines.empty(), CsvLines.empty());
  }
}
