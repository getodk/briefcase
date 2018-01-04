package org.opendatakit.briefcase.ui.export;

import java.util.*;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.*;

import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;

class FormExportTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 7108326237416622721L;
  static final String[] HEADERS = new String[]{"Selected", "Form Name", "Export Status", "Detail"};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int FORM_NAME_COL = 1;
  private static final int EXPORT_STATUS_COL = 2;
  static final int DETAIL_BUTTON_COL = 3;

  private final List<Runnable> selectionChangeCallbacks = new ArrayList<>();
  private List<FormStatus> forms = new ArrayList<>();
  private Map<FormStatus, JButton> detailButtons = new HashMap<>();

  FormExportTableModel() {
    super();
    AnnotationProcessor.process(this);
  }

  void setForms(List<FormStatus> forms) {
    this.forms = forms;
    fireTableDataChanged();
  }

  List<FormStatus> getSelectedForms() {
    return forms.stream().filter(FormStatus::isSelected).collect(Collectors.toList());
  }

  boolean noneSelected() {
    return forms.stream().noneMatch(FormStatus::isSelected);
  }

  boolean allSelected() {
    return forms.stream().allMatch(FormStatus::isSelected);
  }

  void checkAll() {
    for (int row = 0; row < forms.size(); row++)
      setValueAt(true, row, 0);
  }

  void uncheckAll() {
    for (int row = 0; row < forms.size(); row++)
      setValueAt(false, row, 0);
  }

  void onSelectionChange(Runnable callback) {
    selectionChangeCallbacks.add(callback);
  }

  private void selectionChange() {
    selectionChangeCallbacks.forEach(Runnable::run);
  }

  private Optional<Integer> findRow(FormStatus formInEvent) {
    return Optional.of(forms.indexOf(formInEvent)).filter(i -> i != -1);
  }

  private Optional<Integer> findRow(BriefcaseFormDefinition formDefinition) {
    for (int index = 0; index < forms.size(); index++)
      if (forms.get(index).getFormDefinition() == formDefinition)
        return Optional.of(index);
    return Optional.empty();
  }

  private static JButton buildDetailButton(FormStatus form) {
    JButton button = new JButton(HEADERS[3]);
    button.addActionListener(__ -> {
      button.setEnabled(false);
      try {
        showDialog(getFrameForComponent(button), form.getFormDefinition(), form.getStatusHistory());
      } finally {
        button.setEnabled(true);
      }
    });
    return button;
  }

  @Override
  public int getRowCount() {
    return forms.size();
  }

  @Override
  public String getColumnName(int col) {
    return HEADERS[col];
  }

  @Override
  public int getColumnCount() {
    return HEADERS.length;
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Class getColumnClass(int col) {
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        return Boolean.class;
      case FORM_NAME_COL:
        return String.class;
      case EXPORT_STATUS_COL:
        return String.class;
      case DETAIL_BUTTON_COL:
        return JButton.class;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return col == SELECTED_CHECKBOX_COL;
  }

  @Override
  public void setValueAt(Object value, int row, int col) {
    FormStatus form = forms.get(row);
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        form.setSelected((Boolean) value);
        selectionChange();
        break;
      case EXPORT_STATUS_COL:
        form.setStatusString((String) value, true);
        break;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
    fireTableCellUpdated(row, col);
  }

  @Override
  public Object getValueAt(int row, int col) {
    FormStatus form = forms.get(row);
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        return form.isSelected();
      case FORM_NAME_COL:
        return form.getFormName();
      case EXPORT_STATUS_COL:
        return form.getStatusString();
      case DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(form, FormExportTableModel::buildDetailButton);
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

}
