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

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.CsvFieldMappersTest.Scenario.createField;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.encodeMainValue;

import java.util.Arrays;
import java.util.Collection;
import org.hamcrest.Matchers;
import org.javarosa.core.model.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendatakit.briefcase.reused.Pair;

@RunWith(value = Parameterized.class)
public class CsvSubmissionMappersEncodeMainValueTest {
  private static final String ESCAPED_EMPTY_STRING_VALUE = "\"\"";
  private static final String EMPTY_STRING_VALUE = "";
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public Model field;

  @Parameterized.Parameter(value = 2)
  public String expectedOutput;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    // For some reason, empty values are managed differently depending on their related data types
    // This is inconsistent across main or repeat rows, meta fields and missing values, as discussed
    // in https://github.com/opendatakit/briefcase/blob/master/docs/export-format.md#empty-value-codification
    return Arrays.asList(new Object[][]{
        {"GEOPOINT allows empty cols", createField(DataType.GEOPOINT), EMPTY_STRING_VALUE},
        {"DATE allows empty cols", createField(DataType.DATE), EMPTY_STRING_VALUE},
        {"TIME allows empty cols", createField(DataType.TIME), EMPTY_STRING_VALUE},
        {"DATE_TIME allows empty cols", createField(DataType.DATE_TIME), EMPTY_STRING_VALUE},
        {"UNSUPPORTED encodes an empty string", createField(DataType.UNSUPPORTED), ESCAPED_EMPTY_STRING_VALUE},
        {"NULL encodes an empty string", createField(DataType.NULL), ESCAPED_EMPTY_STRING_VALUE},
        {"TEXT encodes an empty string", createField(DataType.TEXT), ESCAPED_EMPTY_STRING_VALUE},
        {"INTEGER encodes an empty string", createField(DataType.INTEGER), ESCAPED_EMPTY_STRING_VALUE},
        {"DECIMAL encodes an empty string", createField(DataType.DECIMAL), ESCAPED_EMPTY_STRING_VALUE},
        {"CHOICE encodes an empty string", createField(DataType.CHOICE), ESCAPED_EMPTY_STRING_VALUE},
        {"MULTIPLE_ITEMS encodes an empty string", createField(DataType.MULTIPLE_ITEMS), ESCAPED_EMPTY_STRING_VALUE},
        {"BOOLEAN encodes an empty string", createField(DataType.BOOLEAN), ESCAPED_EMPTY_STRING_VALUE},
        {"BARCODE encodes an empty string", createField(DataType.BARCODE), ESCAPED_EMPTY_STRING_VALUE},
        {"BINARY encodes an empty string", createField(DataType.BINARY), ESCAPED_EMPTY_STRING_VALUE},
        {"LONG encodes an empty string", createField(DataType.LONG), ESCAPED_EMPTY_STRING_VALUE},
        {"GEOSHAPE encodes an empty string", createField(DataType.GEOSHAPE), ESCAPED_EMPTY_STRING_VALUE},
        {"GEOTRACE encodes an empty string", createField(DataType.GEOTRACE), ESCAPED_EMPTY_STRING_VALUE},
    });
  }

  @Test
  public void fields() {
    assertThat(encodeMainValue(field, Pair.of("some-field", "")), Matchers.is(expectedOutput));
  }
}