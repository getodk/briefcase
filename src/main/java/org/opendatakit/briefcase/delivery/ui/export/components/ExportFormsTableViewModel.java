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

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;
import static java.time.format.FormatStyle.SHORT;
import static org.opendatakit.briefcase.delivery.ui.export.components.ExportFormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.delivery.ui.export.components.ExportFormsTableView.HEADERS;
import static org.opendatakit.briefcase.delivery.ui.export.components.ExportFormsTableView.TYPES;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.delivery.ui.reused.FontUtils;
import org.opendatakit.briefcase.delivery.ui.reused.UI;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportEvent;
import org.opendatakit.briefcase.operations.export.ExportForms;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class ExportFormsTableViewModel extends AbstractTableModel {
  private static final Color NO_CONF_OVERRIDE_COLOR = new Color(0, 128, 0);
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormKey, JButton> detailButtons = new HashMap<>();
  private final Map<FormKey, JButton> confButtons = new HashMap<>();
  private final ExportForms forms;
  private final Map<FormKey, String> statusLines = new ConcurrentHashMap<>();
  private final Map<FormKey, String> lastStatusLine = new ConcurrentHashMap<>();

  private static final Font ic_settings = FontUtils.getCustomFont("ic_settings.ttf", 16f);
  private final Supplier<Boolean> rememberPasswordsGetter;
  private boolean enabled = true;

  ExportFormsTableViewModel(Supplier<Boolean> rememberPasswordsGetter, ExportForms forms) {
    this.rememberPasswordsGetter = rememberPasswordsGetter;
    AnnotationProcessor.process(this);
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

  public void triggerChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  private JButton buildOverrideConfButton(FormMetadata formMetadata, String formName) {
    // Use custom fonts instead of png for easier scaling
    JButton button = new JButton("\uE900");
    button.setFont(ic_settings); // custom font that overrides  with a gear icon
    button.setToolTipText("Override the export configuration for this form");
    button.setMargin(new Insets(0, 0, 0, 0));

    updateConfButton(formMetadata.getKey(), button);
    button.addActionListener(__ -> {
      if (enabled) {
        ConfigurationDialog dialog = ConfigurationDialog.overridePanel(
            forms.getCustomConfiguration(formMetadata.getKey()).orElse(empty().build()),
            formName,
            formMetadata.hasPullSource(),
            rememberPasswordsGetter.get()
        );
        dialog.onRemove(() -> removeConfiguration(formMetadata.getKey()));
        dialog.onOK(configuration -> {
          if (configuration.isEmpty())
            removeConfiguration(formMetadata.getKey());
          else
            putConfiguration(formMetadata.getKey(), configuration);
        });
        dialog.open();
      }
    });
    return button;
  }

  private void putConfiguration(FormKey formKey, ExportConfiguration configuration) {
    forms.putConfiguration(formKey, configuration);
    updateConfButton(formKey, confButtons.get(formKey));
    triggerChange();
  }

  private void removeConfiguration(FormKey formKey) {
    forms.removeConfiguration(formKey);
    updateConfButton(formKey, confButtons.get(formKey));
    triggerChange();
  }

  private void updateDetailButton(FormKey formKey, JButton button) {
    button.setForeground(statusLines.getOrDefault(formKey, "").isBlank() ? LIGHT_GRAY : DARK_GRAY);
  }

  private void updateConfButton(FormKey formKey, JButton button) {
    button.setForeground(forms.hasConfiguration(formKey) ? NO_CONF_OVERRIDE_COLOR : DARK_GRAY);
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
    FormMetadata formMetadata = forms.get(rowIndex);
    FormKey formKey = formMetadata.getKey();
    String formName = formMetadata.getFormName().orElse(formKey.getId());
    switch (columnIndex) {
      case ExportFormsTableView.SELECTED_CHECKBOX_COL:
        return forms.isSelected(formKey);
      case ExportFormsTableView.OVERRIDE_CONF_COL:
        return confButtons.computeIfAbsent(formKey, __ -> this.buildOverrideConfButton(formMetadata, formName));
      case ExportFormsTableView.FORM_NAME_COL:
        return formName;
      case ExportFormsTableView.EXPORT_STATUS_COL:
        return lastStatusLine.getOrDefault(formKey, "");
      case ExportFormsTableView.LAST_EXPORT_COL:
        return forms.getLastExportDateTime(formKey)
            .map(dateTime -> dateTime.format(ofLocalizedDateTime(SHORT, SHORT)))
            .orElse("Not exported yet");
      case ExportFormsTableView.DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(formKey, __ -> UI.buildDetailButton(formKey, formName, () -> statusLines.getOrDefault(formKey, "")));
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
      case ExportFormsTableView.EXPORT_STATUS_COL:
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

  void cleanAllStatusLines() {
    statusLines.clear();
    lastStatusLine.clear();
  }

  @EventSubscriber(eventClass = ExportEvent.class)
  public void onFormStatusEvent(ExportEvent event) {
    String currentStatus = statusLines.computeIfAbsent(event.getFormKey(), key -> "");
    statusLines.put(event.getFormKey(), currentStatus + "\n" + event.getMessage());
    lastStatusLine.put(event.getFormKey(), event.getMessage());
    refresh();
  }
}
