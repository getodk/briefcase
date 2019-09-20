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
package org.opendatakit.briefcase.delivery.ui.export.components;

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataHelpers.buildFormStatusList;

import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opendatakit.briefcase.operations.export.ExportForms;

public class ExportFormsTableUnitTest {
  @Test
  public void can_select_all_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10));
    TestExportFormsTableViewModel viewModel = new TestExportFormsTableViewModel(forms);
    ExportFormsTable formsTable = new ExportFormsTable(forms, new TestExportFormsTableView(viewModel), viewModel);

    assertThat(forms.noneSelected(), Matchers.is(true));

    formsTable.selectAll();

    assertThat(forms.allSelected(), Matchers.is(true));
  }

  @Test
  public void can_clear_selection_of_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10));
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
      super(__ -> Optional.empty(), __ -> empty().build(), () -> true, forms);
    }
  }
}
