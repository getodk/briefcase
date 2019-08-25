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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
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
    CsvLine lastChronological = createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z");
    CsvLine firstChronological = createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z");
    CsvLines csvLines = new CsvLines("some fqdn", Arrays.asList(lastChronological, firstChronological), Optional.of(lastChronological));
    List<String> lines = csvLines.unsorted().collect(toList());
    assertThat(lines, hasSize(2));
    assertThat(lines, contains("2018-01-02T00:00:00.000Z", "2018-01-01T00:00:00.000Z"));
  }

  @Test
  public void lines_can_be_obtained_sorted_by_submission_date() {
    CsvLine lastChronological = createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z");
    CsvLine firstChronological = createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z");
    CsvLines csvLines = new CsvLines("some fqdn", Arrays.asList(lastChronological, firstChronological), Optional.of(lastChronological));
    List<String> lines = csvLines.sorted().collect(toList());
    assertThat(lines, hasSize(2));
    assertThat(lines, contains("2018-01-01T00:00:00.000Z", "2018-01-02T00:00:00.000Z"));
  }

  @Test
  public void instances_with_the_same_fqdn_can_be_merged() {
    CsvLine lastChronological = createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z");
    CsvLine firstChronological = createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z");
    CsvLines csvLines1 = new CsvLines("some fqdn", singletonList(firstChronological), Optional.of(firstChronological));
    CsvLines csvLines2 = new CsvLines("some fqdn", singletonList(lastChronological), Optional.of(lastChronological));
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
    CsvLine csvLine = createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z");
    CsvLines nonEmpty = new CsvLines("some fqdn", singletonList(csvLine), Optional.of(csvLine));

    CsvLines leftWasEmpty = CsvLines.merge(empty, nonEmpty);
    assertThat(leftWasEmpty.getModelFqn(), is("some fqdn"));
    assertThat(leftWasEmpty.unsorted().collect(toList()), hasSize(1));

    CsvLines rightWasEmpty = CsvLines.merge(nonEmpty, empty);
    assertThat(rightWasEmpty.getModelFqn(), is("some fqdn"));
    assertThat(rightWasEmpty.unsorted().collect(toList()), hasSize(1));
  }

  @Test(expected = BriefcaseException.class)
  public void throws_when_merging_two_instances_with_non_matching_fqdns() {
    CsvLine lastChronological = createCsvLine("uuid:1234", "2018-01-02T00:00:00.000Z");
    CsvLine firstChronological = createCsvLine("uuid:1234", "2018-01-01T00:00:00.000Z");
    CsvLines csvLines1 = new CsvLines("some fqdn", singletonList(firstChronological), Optional.of(firstChronological));
    CsvLines csvLines2 = new CsvLines("some other fqdn", singletonList(lastChronological), Optional.of(lastChronological));
    CsvLines.merge(csvLines1, csvLines2);
  }

  @Test(expected = BriefcaseException.class)
  public void throws_when_merging_two_instances_without_fqdns() {
    CsvLines.merge(CsvLines.empty(), CsvLines.empty());
  }

  @Test
  public void remembers_which_csv_line_is_the_last_one_chronologically_while_merging() {
    OffsetDateTime startDateTime = OffsetDateTime.parse("2019-01-01T00:00:00.000Z");
    List<OffsetDateTime> dateTimes = IntStream.range(1, 31)
        .mapToObj(startDateTime::plusDays)
        .collect(toList());
    OffsetDateTime lastDateTime = dateTimes.stream().max(OffsetDateTime::compareTo).get();

    // Shuffle so that we make sure the merge method has to handle unordered lines
    Collections.shuffle(dateTimes);

    CsvLines mergedCsvLines = dateTimes.stream()
        .map(submissionDate -> CsvLines.of("some_model", "uuid:1234", submissionDate, "some line"))
        .reduce(CsvLines.empty(), CsvLines::merge);

    assertThat(mergedCsvLines.getLastLine(), isPresentAndIs(new CsvLine("uuid:1234", lastDateTime, "some line")));
  }
}
