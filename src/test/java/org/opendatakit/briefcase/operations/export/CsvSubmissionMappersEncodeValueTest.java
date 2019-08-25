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

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.operations.export.CsvSubmissionMappers.encodeMainValue;
import static org.opendatakit.briefcase.operations.export.CsvSubmissionMappers.encodeRepeatValue;
import static org.opendatakit.briefcase.operations.export.Scenario.createField;

import org.hamcrest.Matchers;
import org.javarosa.core.model.DataType;
import org.junit.Test;
import org.opendatakit.briefcase.reused.api.Pair;

/**
 * This test class complements {@link CsvSubmissionMappersEncodeMainValueTest}
 */
public class CsvSubmissionMappersEncodeValueTest {

  @Test
  public void meta_fields_in_main_rows_allow_empty_cols() {
    // Text fields would normally avoid an empty column by encoding an empty string
    assertThat(encodeMainValue(createField(DataType.TEXT), Pair.of("meta-some-field", "")), Matchers.is(""));
  }

  @Test
  public void meta_fields_in_repeat_rows_allow_empty_cols() {
    assertThat(encodeRepeatValue(Pair.of("meta-some-field", "")), Matchers.is(""));
  }

  @Test
  public void set_of_fields_in_repeat_rows_allow_empty_cols() {
    assertThat(encodeRepeatValue(Pair.of("SET-OF", "")), Matchers.is(""));
  }

  @Test
  public void otherwise_repeat_rows_avoid_empty_cols() {
    assertThat(encodeRepeatValue(Pair.of("some-field", "")), Matchers.is("\"\""));
  }
}
