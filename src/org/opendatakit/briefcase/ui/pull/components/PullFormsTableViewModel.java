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

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;
import static org.opendatakit.briefcase.ui.pull.components.PullFormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.ui.pull.components.PullFormsTableView.HEADERS;
import static org.opendatakit.briefcase.ui.pull.components.PullFormsTableView.TYPES;

import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.pull.PullForms;
import org.opendatakit.briefcase.ui.export.components.ExportFormsTableView;
import org.opendatakit.briefcase.ui.reused.FontUtils;

public class PullFormsTableViewModel extends AbstractTableModel {
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormStatus, JButton> detailButtons = new HashMap<>();
  private final PullForms forms;

  private static final Font ic_receipt = FontUtils.getCustomFont("ic_receipt.ttf", 16f);

  public PullFormsTableViewModel(PullForms forms) {
    this.forms = forms;
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

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  private JButton buildDetailButton(FormStatus form) {
    // Use custom fonts instead of png for easier scaling
    JButton button = new JButton("\uE900");
    button.setFont(ic_receipt); // custom font that overrides  with a receipt icon
    button.setToolTipText("View this form's status history");
    button.setMargin(new Insets(0, 0, 0, 0));

    button.setForeground(LIGHT_GRAY);
    button.addActionListener(__ -> {
      if (!form.getStatusHistory().isEmpty())
        showDialog(getFrameForComponent(button), form.getFormDefinition(), form.getStatusHistory());
    });
    return button;
  }

  private void updateDetailButton(FormStatus form, JButton button) {
    button.setForeground(form.getStatusHistory().isEmpty() ? LIGHT_GRAY : DARK_GRAY);
  }

  @Override
  public int getRowCount() {
    return forms.size();
  }

  @Override
  public int getColumnCount() {
    return HEADERS.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    FormStatus form = forms.get(rowIndex);
    switch (columnIndex) {
      case PullFormsTableView.SELECTED_CHECKBOX_COL:
        return form.isSelected();
      case PullFormsTableView.FORM_NAME_COL:
        return form.getFormName();
      case PullFormsTableView.PULL_STATUS_COL:
        return form.getStatusString();
      case PullFormsTableView.DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(form, this::buildDetailButton);
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @Override
  // Suppressing next ParameterName checkstyle error becasue 'aValue' param triggers it by mistake
  @SuppressWarnings("checkstyle:ParameterName")
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    FormStatus form = forms.get(rowIndex);
    switch (columnIndex) {
      case ExportFormsTableView.SELECTED_CHECKBOX_COL:
        Boolean isSelected = (Boolean) aValue;
        form.setSelected(isSelected);
        triggerChange();
        break;
      case PullFormsTableView.PULL_STATUS_COL:
        form.setStatusString((String) aValue, true);
        break;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  @Override
  public String getColumnName(int column) {
    return HEADERS[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return TYPES[columnIndex];
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return EDITABLE_COLS[columnIndex];
  }

}
