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
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.ui.export.components.ExportFormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.ui.export.components.ExportFormsTableView.HEADERS;
import static org.opendatakit.briefcase.ui.export.components.ExportFormsTableView.TYPES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;
import org.opendatakit.briefcase.ui.reused.ExportConfigurationButton;
import org.opendatakit.briefcase.ui.reused.UI;

public class ExportFormsTableViewModel extends AbstractTableModel {
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormStatus, JButton> detailButtons = new HashMap<>();
  private final Map<FormStatus, ExportConfigurationButton> confButtons = new HashMap<>();
  private final ExportForms forms;
  private final BriefcasePreferences appPreferences;
  private BriefcasePreferences pullPrefs;

  private boolean enabled = true;

  ExportFormsTableViewModel(ExportForms forms, BriefcasePreferences appPreferences, BriefcasePreferences pullPrefs) {
    this.forms = forms;
    this.appPreferences = appPreferences;
    this.pullPrefs = pullPrefs;
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  void refresh() {
    detailButtons.forEach(this::updateDetailButton);
    fireTableDataChanged();
    triggerChange();
  }

  public void triggerChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  private ExportConfigurationButton buildOverrideConfButton(FormStatus form) {
    ExportConfigurationButton button = new ExportConfigurationButton();

    updateConfButton(form, button);
    button.addActionListener(__ -> {
      if (enabled) {
        ConfigurationDialog dialog = ConfigurationDialog.overridePanel(
            forms.getCustomConfiguration(form).orElse(empty().build()),
            form.getFormName(),
            RemoteServer.readFromPrefs(appPreferences, pullPrefs, form).isPresent(),
            BriefcasePreferences.getStorePasswordsConsentProperty()
        );
        dialog.onRemove(() -> removeConfiguration(form));
        dialog.onOK(configuration -> {
          if (configuration.isEmpty())
            removeConfiguration(form);
          else
            putConfiguration(form, configuration);
        });
        dialog.open();
      }
    });
    return button;
  }

  private void putConfiguration(FormStatus form, ExportConfiguration configuration) {
    forms.putConfiguration(form, configuration);
    updateConfButton(form, confButtons.get(form));
    triggerChange();
  }

  private void removeConfiguration(FormStatus form) {
    forms.removeConfiguration(form);
    updateConfButton(form, confButtons.get(form));
    triggerChange();
  }

  private void updateDetailButton(FormStatus form, JButton button) {
    button.setForeground(form.getStatusHistory().isEmpty() ? LIGHT_GRAY : DARK_GRAY);
  }

  private void updateConfButton(FormStatus form, ExportConfigurationButton button) {
    button.setConfigured(forms.hasConfiguration(form));
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
      case ExportFormsTableView.SELECTED_CHECKBOX_COL:
        return form.isSelected();
      case ExportFormsTableView.OVERRIDE_CONF_COL:
        return confButtons.computeIfAbsent(form, this::buildOverrideConfButton);
      case ExportFormsTableView.FORM_NAME_COL:
        return form.getFormName();
      case ExportFormsTableView.EXPORT_STATUS_COL:
        return form.getStatusString();
      case ExportFormsTableView.LAST_EXPORT_COL:
        return forms.getLastExportDateTime(form)
            .map(dateTime -> dateTime.format(ofLocalizedDateTime(SHORT, SHORT)))
            .orElse("Not exported yet");
      case ExportFormsTableView.DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(form, UI::buildDetailButton);
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @Override
  // Suppressing next ParameterName checkstyle error because 'aValue' param triggers it by mistake
  @SuppressWarnings("checkstyle:ParameterName")
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    FormStatus form = forms.get(rowIndex);
    switch (columnIndex) {
      case ExportFormsTableView.SELECTED_CHECKBOX_COL:
        Boolean isSelected = (Boolean) aValue;
        form.setSelected(isSelected);
        triggerChange();
        break;
      case ExportFormsTableView.EXPORT_STATUS_COL:
        form.setStatusString((String) aValue);
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

  @SuppressWarnings("checkstyle:ParameterName")
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    confButtons.forEach((__, button) -> button.setEnabled(enabled));
  }
}
