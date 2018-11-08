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
package org.opendatakit.briefcase.ui.export.components;

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.model.FormStatusBuilder.buildFormStatusList;

import java.util.HashMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportForms;

public class ExportFormsTableUnitTest {
  @Test
  public void can_select_all_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), empty().build(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    TestExportFormsTableViewModel viewModel = new TestExportFormsTableViewModel(forms);
    ExportFormsTable formsTable = new ExportFormsTable(forms, new TestExportFormsTableView(viewModel), viewModel);

    assertThat(forms.noneSelected(), Matchers.is(true));

    formsTable.selectAll();

    assertThat(forms.allSelected(), Matchers.is(true));
  }

  @Test
  public void can_clear_selection_of_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), empty().build(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    TestExportFormsTableViewModel viewModel = new TestExportFormsTableViewModel(forms);
    ExportFormsTable formsTable = new ExportFormsTable(forms, new TestExportFormsTableView(viewModel), viewModel);
    formsTable.selectAll();

    formsTable.clearAll();

    assertThat(forms.noneSelected(), Matchers.is(true));
  }

  private class TestExportFormsTableView extends ExportFormsTableView {
    TestExportFormsTableView(ExportFormsTableViewModel model) {
      super(model);
    }
  }

  private class TestExportFormsTableViewModel extends ExportFormsTableViewModel {
    TestExportFormsTableViewModel(ExportForms forms) {
      super(forms);
    }
  }
}