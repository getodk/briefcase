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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataHelpers.buildFormStatusList;

import org.junit.Test;

public class ExportFormsTest {
  @Test
  public void manages_forms_selection() {
    ExportForms forms = new ExportForms(buildFormStatusList(10));
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));

    forms.setSelected(forms.get(0).getKey(), true);
    assertThat(forms.getSelectedForms(), hasSize(1));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.selectAll();
    assertThat(forms.getSelectedForms(), hasSize(10));
    assertThat(forms.allSelected(), is(true));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.clearAll();
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));
  }
}
