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
package org.opendatakit.briefcase.ui.reused.transfer;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.transfer.TransferForms;

public class TransferFormsTable {
  private final TransferFormsTableView view;
  private final TransferFormsTableViewModel viewModel;
  private final TransferForms forms;

  private TransferFormsTable(TransferForms forms, TransferFormsTableView view, TransferFormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static TransferFormsTable from(TransferForms forms, String actionName) {
    String[] headers = TransferFormsTableView.buildHeaders(actionName);
    TransferFormsTableViewModel viewModel = new TransferFormsTableViewModel(forms, headers);
    return new TransferFormsTable(forms, new TransferFormsTableView(viewModel, headers), viewModel);
  }

  public void onChange(Runnable callback) {
    viewModel.onChange(callback);
  }

  void selectAll() {
    forms.selectAll();
    viewModel.refresh();
  }

  void clearAll() {
    forms.clearAll();
    viewModel.refresh();
  }

  void refresh() {
    viewModel.refresh();
  }

  public TransferFormsTableView getView() {
    return view;
  }

  void enableInteraction() {
    view.setEnabled(true);
  }

  void disableInteraction() {
    view.setEnabled(false);
  }
}
