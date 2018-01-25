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

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;
import static java.time.format.FormatStyle.SHORT;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.HEADERS;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.TYPES;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.ui.reused.FontUtils;

public class FormsTableViewModel extends AbstractTableModel {
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormStatus, JButton> detailButtons = new HashMap<>();
  private final Map<FormStatus, JButton> confButtons = new HashMap<>();
  private final ExportForms forms;

  private static final Font ic_settings = FontUtils.getCustomFont("ic_settings.ttf", 16f);

  FormsTableViewModel(ExportForms forms) {
    this.forms = forms;
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  void refresh() {
    detailButtons.forEach((form, button) -> button.setEnabled(!form.getStatusHistory().isEmpty()));
    fireTableDataChanged();
    triggerChange();
  }

  public void triggerChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  JButton buildDetailButton(FormStatus form) {
    JButton button = new JButton("Details...");
    button.setEnabled(false);
    // Ugly hack to be able to use this factory in FormExportTable to compute its Dimension
    if (form != null) {
      button.addActionListener(__ -> {
        button.setEnabled(false);
        try {
          showDialog(getFrameForComponent(button), form.getFormDefinition(), form.getStatusHistory());
        } finally {
          button.setEnabled(true);
        }
      });
    }
    return button;
  }

  JButton buildOverrideConfButton(FormStatus form) {

    // Use custom fonts instead of png for easier scaling
    JButton button = new JButton("\uE900"); // custom font that overrides  with a ⚙️
    button.setFont(ic_settings);

    // Ugly hack to be able to use this factory in FormExportTable to compute its Dimension
    if (form != null) {
      button.setForeground(forms.hasConfiguration(form) ? DARK_GRAY : LIGHT_GRAY);
      button.addActionListener(__ -> {
        button.setEnabled(false);
        try {
          ConfigurationDialog dialog = ConfigurationDialog.from(forms.getCustomConfiguration(form));
          dialog.onRemove(() -> removeConfiguration(form));
          dialog.onOK(configuration -> {
            if (configuration.isEmpty())
              removeConfiguration(form);
            else
              putConfiguration(form, configuration);
          });
          dialog.open();
        } finally {
          button.setEnabled(true);
        }
      });
    }
    return button;
  }

  private void putConfiguration(FormStatus form, ExportConfiguration configuration) {
    forms.putConfiguration(form, configuration);
    confButtons.get(form).setForeground(DARK_GRAY);
    triggerChange();
  }

  private void removeConfiguration(FormStatus form) {
    forms.removeConfiguration(form);
    confButtons.get(form).setForeground(LIGHT_GRAY);
    triggerChange();
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
      case FormsTableView.SELECTED_CHECKBOX_COL:
        return form.isSelected();
      case FormsTableView.OVERRIDE_CONF_COL:
        return confButtons.computeIfAbsent(form, this::buildOverrideConfButton);
      case FormsTableView.FORM_NAME_COL:
        return form.getFormName();
      case FormsTableView.EXPORT_STATUS_COL:
        return form.getStatusString();
      case FormsTableView.LAST_EXPORT_COL:
        return forms.getLastExportDateTime(form)
            .map(dateTime -> dateTime.format(ofLocalizedDateTime(SHORT, SHORT)))
            .orElse("Not exported yet");
      case FormsTableView.DETAIL_BUTTON_COL:
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
      case FormsTableView.SELECTED_CHECKBOX_COL:
        Boolean isSelected = (Boolean) aValue;
        form.setSelected(isSelected);
        triggerChange();
        break;
      case FormsTableView.EXPORT_STATUS_COL:
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
