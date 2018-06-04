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
package org.opendatakit.briefcase.ui.push.components;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.push.PushForms;

public class PushFormsTable {
  private final PushFormsTableView view;
  private final PushFormsTableViewModel viewModel;
  private final PushForms forms;

  public PushFormsTable(PushForms forms, PushFormsTableView view, PushFormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static PushFormsTable from(PushForms forms) {
    PushFormsTableViewModel viewModel = new PushFormsTableViewModel(forms);
    return new PushFormsTable(forms, new PushFormsTableView(viewModel), viewModel);
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

  public PushFormsTableView getView() {
    return view;
  }

  public void enableInteraction() {
    view.setEnabled(true);
  }

  public void disableInteraction() {
    view.setEnabled(false);
  }
}
