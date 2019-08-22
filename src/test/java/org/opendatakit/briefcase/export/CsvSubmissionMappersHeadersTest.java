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
import static org.opendatakit.briefcase.export.ModelBuilder.geopoint;
import static org.opendatakit.briefcase.export.ModelBuilder.group;
import static org.opendatakit.briefcase.export.ModelBuilder.instance;
import static org.opendatakit.briefcase.export.ModelBuilder.repeat;
import static org.opendatakit.briefcase.export.ModelBuilder.selectMultiple;
import static org.opendatakit.briefcase.export.ModelBuilder.text;

import org.javarosa.core.model.SelectChoice;
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

  @Test
  public void supports_dupe_field_names() {
    Model model = instance(
        group("group-1", text("field")),
        group("group-2", text("field"))
    ).build();
    assertThat(getMainHeader(model, false, false, false), is("SubmissionDate,group-1-field,group-2-field,KEY"));
    assertThat(getMainHeader(model, true, false, false), is("SubmissionDate,group-1-field,group-2-field,KEY,isValidated"));
    assertThat(getMainHeader(model, false, false, true), is("SubmissionDate,field,field,KEY"));
    assertThat(getMainHeader(model, true, false, true), is("SubmissionDate,field,field,KEY,isValidated"));
  }

  @Test
  public void supports_geopoint_fields() {
    Model model = instance(
        group("some-group", geopoint("some-point"))
    ).build();
    assertThat(getMainHeader(model, false, false, false), is("SubmissionDate,some-group-some-point-Latitude,some-group-some-point-Longitude,some-group-some-point-Altitude,some-group-some-point-Accuracy,KEY"));
    assertThat(getMainHeader(model, true, false, false), is("SubmissionDate,some-group-some-point-Latitude,some-group-some-point-Longitude,some-group-some-point-Altitude,some-group-some-point-Accuracy,KEY,isValidated"));
    assertThat(getMainHeader(model, false, false, true), is("SubmissionDate,some-point-Latitude,some-point-Longitude,some-point-Altitude,some-point-Accuracy,KEY"));
    assertThat(getMainHeader(model, true, false, true), is("SubmissionDate,some-point-Latitude,some-point-Longitude,some-point-Altitude,some-point-Accuracy,KEY,isValidated"));
  }

  @Test
  public void supports_splitting_select_multiple_fields() {
    SelectChoice choice1 = new SelectChoice("some label 1", "some value 1", false);
    SelectChoice choice2 = new SelectChoice("some label 2", "some value 2", false);

    Model model = instance(selectMultiple("select", choice1, choice2)).build();
    assertThat(getMainHeader(model, false, false, false), is("SubmissionDate,select,KEY"));
    assertThat(getMainHeader(model, true, false, false), is("SubmissionDate,select,KEY,isValidated"));
    assertThat(getMainHeader(model, false, false, true), is("SubmissionDate,select,KEY"));
    assertThat(getMainHeader(model, true, false, true), is("SubmissionDate,select,KEY,isValidated"));
    assertThat(getMainHeader(model, false, true, false), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY"));
    assertThat(getMainHeader(model, true, true, false), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY,isValidated"));
    assertThat(getMainHeader(model, false, true, true), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY"));
    assertThat(getMainHeader(model, true, true, true), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY,isValidated"));
    Model repeat = instance(repeat("some-repeat", selectMultiple("select", choice1, choice2))).build().getChildByName("some-repeat");
    assertThat(getRepeatHeader(repeat, false, false), is("select,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, false, true), is("select,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, true, false), is("select,select/some value 1,select/some value 2,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, true, true), is("select,select/some value 1,select/some value 2,PARENT_KEY,KEY,SET-OF-some-repeat"));
  }

  @Test
  public void supports_splitting_select_multiple_fields_in_groups() {
    SelectChoice choice1 = new SelectChoice("some label 1", "some value 1", false);
    SelectChoice choice2 = new SelectChoice("some label 2", "some value 2", false);

    Model model = instance(group("some-group", selectMultiple("select", choice1, choice2))).build();
    assertThat(getMainHeader(model, false, false, false), is("SubmissionDate,some-group-select,KEY"));
    assertThat(getMainHeader(model, true, false, false), is("SubmissionDate,some-group-select,KEY,isValidated"));
    assertThat(getMainHeader(model, false, false, true), is("SubmissionDate,select,KEY"));
    assertThat(getMainHeader(model, true, false, true), is("SubmissionDate,select,KEY,isValidated"));
    assertThat(getMainHeader(model, false, true, false), is("SubmissionDate,some-group-select,some-group-select/some value 1,some-group-select/some value 2,KEY"));
    assertThat(getMainHeader(model, true, true, false), is("SubmissionDate,some-group-select,some-group-select/some value 1,some-group-select/some value 2,KEY,isValidated"));
    assertThat(getMainHeader(model, false, true, true), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY"));
    assertThat(getMainHeader(model, true, true, true), is("SubmissionDate,select,select/some value 1,select/some value 2,KEY,isValidated"));
    Model repeat = instance(repeat("some-repeat", group("some-group", selectMultiple("select", choice1, choice2)))).build().getChildByName("some-repeat");
    assertThat(getRepeatHeader(repeat, false, false), is("some-group-select,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, false, true), is("select,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, true, false), is("some-group-select,some-group-select/some value 1,some-group-select/some value 2,PARENT_KEY,KEY,SET-OF-some-repeat"));
    assertThat(getRepeatHeader(repeat, true, true), is("select,select/some value 1,select/some value 2,PARENT_KEY,KEY,SET-OF-some-repeat"));
  }
}
