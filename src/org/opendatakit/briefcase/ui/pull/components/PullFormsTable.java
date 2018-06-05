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
package org.opendatakit.briefcase.ui.pull.components;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.pull.PullForms;

public class PullFormsTable {
  private final PullFormsTableView view;
  private final PullFormsTableViewModel viewModel;
  private final PullForms forms;

  public PullFormsTable(PullForms forms, PullFormsTableView view, PullFormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static PullFormsTable from(PullForms forms) {
    String[] headers = PullFormsTableView.buildHeaders("Pull");
    PullFormsTableViewModel viewModel = new PullFormsTableViewModel(forms, headers);
    return new PullFormsTable(forms, new PullFormsTableView(viewModel, headers), viewModel);
  }

  public void onChange(Runnable callback) {
    viewModel.onChange(callback);
  }

  public void selectAll() {
    forms.selectAll();
    viewModel.refresh();
  }

  public void clearAll() {
    forms.clearAll();
    viewModel.refresh();
  }

  public void refresh() {
    viewModel.refresh();
  }

  public PullFormsTableView getView() {
    return view;
  }

  public void enableInteraction() {
    view.setEnabled(true);
  }

  public void disableInteraction() {
    view.setEnabled(false);
  }
}
