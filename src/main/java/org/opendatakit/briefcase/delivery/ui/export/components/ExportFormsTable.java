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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportEvent;
import org.opendatakit.briefcase.operations.export.ExportForms;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class ExportFormsTable {
  private final ExportFormsTableView view;
  private final ExportFormsTableViewModel viewModel;
  private final ExportForms forms;

  ExportFormsTable(ExportForms forms, ExportFormsTableView view, ExportFormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static ExportFormsTable from(Function<FormKey, Optional<OffsetDateTime>> lastExportDateTimeGetter, Function<FormKey, ExportConfiguration> configurationGetter, Supplier<Boolean> rememberPasswordsGetter, ExportForms forms) {
    ExportFormsTableViewModel viewModel = new ExportFormsTableViewModel(lastExportDateTimeGetter, configurationGetter, rememberPasswordsGetter, forms);
    return new ExportFormsTable(forms, new ExportFormsTableView(viewModel), viewModel);
  }

  public void onChange(Runnable callback) {
    viewModel.onChange(callback);
  }

  public void onConfigurationSet(BiConsumer<FormMetadata, ExportConfiguration> callback) {
    viewModel.onConfigurationSet(callback);
  }

  public void onConfigurationReset(Consumer<FormMetadata> callback) {
    viewModel.onConfigurationReset(callback);
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

  public ExportFormsTableView getView() {
    return view;
  }

  public ExportFormsTableViewModel getViewModel() {
    return viewModel;
  }

  public void setEnabled(boolean enabled) {
    view.setEnabled(enabled);
    viewModel.setEnabled(enabled);
  }

  @EventSubscriber(eventClass = ExportEvent.class)
  public void onExportEvent(ExportEvent event) {
    viewModel.refresh();
  }
}
