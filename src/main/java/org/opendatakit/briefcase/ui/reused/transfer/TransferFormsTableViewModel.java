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

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static org.opendatakit.briefcase.ui.reused.transfer.TransferFormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.ui.reused.transfer.TransferFormsTableView.TYPES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.reused.Operation;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.export.components.ExportFormsTableView;
import org.opendatakit.briefcase.ui.reused.UI;

public class TransferFormsTableViewModel extends AbstractTableModel {
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormKey, JButton> detailButtons = new HashMap<>();
  private final TransferForms forms;
  private final String[] headers;
  private final Map<FormKey, String> statusLines = new ConcurrentHashMap<>();
  private final Map<FormKey, String> lastStatusLine = new ConcurrentHashMap<>();
  private final Operation operation;

  TransferFormsTableViewModel(TransferForms forms, String[] headers, Operation operation) {
    AnnotationProcessor.process(this);
    this.operation = operation;
    this.forms = forms;
    this.headers = headers;
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  void refresh() {
    detailButtons.forEach(this::updateDetailButton);
    fireTableDataChanged();
    triggerChange();
  }

  private void triggerChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  private void updateDetailButton(FormKey formKey, JButton button) {
    button.setForeground(statusLines.getOrDefault(formKey, "").isBlank() ? LIGHT_GRAY : DARK_GRAY);
  }

  @Override
  public int getRowCount() {
    return forms.size();
  }

  @Override
  public int getColumnCount() {
    return headers.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    FormKey formKey = forms.get(rowIndex).getKey();
    switch (columnIndex) {
      case TransferFormsTableView.SELECTED_CHECKBOX_COL:
        return forms.isSelected(formKey);
      case TransferFormsTableView.FORM_NAME_COL:
        return formKey.getName();
      case TransferFormsTableView.STATUS_COL:
        return lastStatusLine.getOrDefault(formKey, "");
      case TransferFormsTableView.DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(formKey, __ -> UI.buildDetailButton(formKey, () -> statusLines.getOrDefault(formKey, "")));
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @Override
  // Suppressing next ParameterName checkstyle error becasue 'aValue' param triggers it by mistake
  @SuppressWarnings("checkstyle:ParameterName")
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    FormKey formKey = forms.get(rowIndex).getKey();
    switch (columnIndex) {
      case ExportFormsTableView.SELECTED_CHECKBOX_COL:
        forms.setSelected(formKey, (Boolean) aValue);
        triggerChange();
        break;
      case TransferFormsTableView.STATUS_COL:
        // TODO WHAT IS THIS?
        System.out.println("WTF!");
        break;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  @Override
  public String getColumnName(int column) {
    return headers[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return TYPES[columnIndex];
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return EDITABLE_COLS[columnIndex];
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    if (event.getOperation() == operation) {
      String currentStatus = statusLines.computeIfAbsent(event.getFormKey(), key -> "");
      statusLines.put(event.getFormKey(), currentStatus + "\n" + event.getMessage());
      lastStatusLine.put(event.getFormKey(), event.getMessage());
      refresh();
    }
  }

  void clearAllStatusLines() {
    statusLines.clear();
    lastStatusLine.clear();
    refresh();
  }
}
