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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getMainHeader;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getRepeatHeader;
import static org.opendatakit.briefcase.export.ModelBuilder.group;
import static org.opendatakit.briefcase.export.ModelBuilder.instance;
import static org.opendatakit.briefcase.export.ModelBuilder.repeat;
import static org.opendatakit.briefcase.export.ModelBuilder.text;

import org.junit.Test;

public class CsvSubmissionMappersHeadersTest {

  @Test
  public void produces_a_header_for_the_main_file() {
    Model model = instance(group("group", text("field"))).build();
    assertThat(getMainHeader(model, false, false, false), is("SubmissionDate,group-field,KEY"));
    assertThat(getMainHeader(model, true, false, false), is("SubmissionDate,group-field,KEY,isValidated"));
    assertThat(getMainHeader(model, false, false, true), is("SubmissionDate,field,KEY"));
    assertThat(getMainHeader(model, true, false, true), is("SubmissionDate,field,KEY,isValidated"));
  }

  @Test
  public void produces_a_header_for_a_repeat_file() {
    Model repeat = instance(repeat("repeat", group("group", text("field")))).build().getChildByName("repeat");
    assertThat(getRepeatHeader(repeat, false, false), is("group-field,PARENT_KEY,KEY,SET-OF-repeat"));
    assertThat(getRepeatHeader(repeat, false, true), is("field,PARENT_KEY,KEY,SET-OF-repeat"));
  }
}
