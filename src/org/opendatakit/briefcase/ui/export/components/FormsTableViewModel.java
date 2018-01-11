package org.opendatakit.briefcase.ui.export.components;

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.GREEN;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.EDITABLE_COLS;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.HEADERS;
import static org.opendatakit.briefcase.ui.export.components.FormsTableView.TYPES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;
import org.opendatakit.briefcase.ui.export.ExportForms;

class FormsTableViewModel extends AbstractTableModel {
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final Map<FormStatus, JButton> detailButtons = new HashMap<>();
  private final Map<FormStatus, JButton> confButtons = new HashMap<>();
  private final ExportForms forms;
  private boolean enabled;

  FormsTableViewModel(ExportForms forms) {
    this.forms = forms;
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  public void enable() {
    enabled = true;
    confButtons.forEach((form, button) -> button.setEnabled(form.isSelected()));
    detailButtons.forEach((form, button) -> button.setEnabled(!form.getStatusHistory().isEmpty()));
    fireTableDataChanged();
  }

  public void disable() {
    enabled = false;
    confButtons.values().forEach(button -> button.setEnabled(false));
    detailButtons.values().forEach(button -> button.setEnabled(false));
    fireTableDataChanged();
  }

  void refresh() {
    enable();
    triggerChange();
  }

  private void triggerChange() {
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
    JButton button = new JButton("⚙");
    button.setEnabled(false);
    if (forms.hasConfiguration(form))
      button.setForeground(GREEN);
    // Ugly hack to be able to use this factory in FormExportTable to compute its Dimension
    if (form != null) {
      button.addActionListener(__ -> {
        button.setEnabled(false);
        try {
          ConfigurationDialog dialog = new ConfigurationDialog(getFrameForComponent(button), forms.getConfiguration(form));
          dialog.onRemove(() -> removeConfiguration(form));
          dialog.onApply(configuration -> applyConfiguration(form, configuration));
          dialog.open();
        } finally {
          button.setEnabled(true);
        }
      });
    }
    return button;
  }

  private void applyConfiguration(FormStatus form, ExportConfiguration configuration) {
    forms.setConfiguration(form, configuration);
    confButtons.get(form).setForeground(GREEN);
    triggerChange();
  }

  private void removeConfiguration(FormStatus form) {
    forms.removeConfiguration(form);
    confButtons.get(form).setForeground(DARK_GRAY);
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
        confButtons.get(form).setEnabled(isSelected);
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
    return enabled && EDITABLE_COLS[columnIndex];
  }


}
