package org.opendatakit.briefcase.ui.export.components;

import static javax.swing.SortOrder.ASCENDING;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.opendatakit.briefcase.ui.reused.MouseListenerBuilder;

public class FormsTableView extends JTable {
  static final String[] HEADERS = new String[]{"Selected", "⚙", "Form Name", "Export Status", "Detail"};
  static final Class[] TYPES = new Class[]{Boolean.class, JButton.class, String.class, String.class, JButton.class};
  static final boolean[] EDITABLE_COLS = new boolean[]{true, false, false, false, false};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int OVERRIDE_CONF_COL = 1;
  static final int FORM_NAME_COL = 2;
  static final int EXPORT_STATUS_COL = 3;
  static final int DETAIL_BUTTON_COL = 4;

  FormsTableView(FormsTableViewModel model) {
    super(model);

    addMouseListener(new MouseListenerBuilder().onClick(this::relayClickToButton).build());

    Dimension checkboxDims = getTableHeader()
        .getDefaultRenderer()
        .getTableCellRendererComponent(null, HEADERS[SELECTED_CHECKBOX_COL], false, false, 0, 0)
        .getPreferredSize();
    Dimension detailButtonDims = model.buildDetailButton(null).getPreferredSize();
    Dimension overrideConfButtonDims = model.buildOverrideConfButton(null).getPreferredSize();

    setRowHeight(detailButtonDims.height);

    TableColumnModel columns = getColumnModel();
    columns.getColumn(SELECTED_CHECKBOX_COL).setMinWidth(checkboxDims.width);
    columns.getColumn(SELECTED_CHECKBOX_COL).setMaxWidth(checkboxDims.width);
    columns.getColumn(SELECTED_CHECKBOX_COL).setPreferredWidth(checkboxDims.width);
    columns.getColumn(OVERRIDE_CONF_COL).setCellRenderer(cellWithButton());
    columns.getColumn(OVERRIDE_CONF_COL).setMinWidth(overrideConfButtonDims.width + 5);
    columns.getColumn(OVERRIDE_CONF_COL).setMaxWidth(overrideConfButtonDims.width + 5);
    columns.getColumn(OVERRIDE_CONF_COL).setPreferredWidth(overrideConfButtonDims.width + 5);
    columns.getColumn(DETAIL_BUTTON_COL).setCellRenderer(cellWithButton());
    columns.getColumn(DETAIL_BUTTON_COL).setMinWidth(detailButtonDims.width);
    columns.getColumn(DETAIL_BUTTON_COL).setMaxWidth(detailButtonDims.width);
    columns.getColumn(DETAIL_BUTTON_COL).setPreferredWidth(detailButtonDims.width);

    setFillsViewportHeight(true);

    TableRowSorter<FormsTableViewModel> sorter = sortBy(getModel(), FORM_NAME_COL, ASCENDING);
    setRowSorter(sorter);
    sorter.sort();
  }


  void refresh() {
    getModel().refresh();
  }

  private void relayClickToButton(MouseEvent event) {
    int column = getColumnModel().getColumnIndexAtX(event.getX());
    int row = event.getY() / getRowHeight();

    if (row < getRowCount() && row >= 0 && column < getColumnCount() && column >= 0) {
      Object value = getValueAt(row, column);
      if (value instanceof JButton)
        ((JButton) value).doClick();
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled)
      getModel().enable();
    else
      getModel().disable();
  }

  @Override
  public FormsTableViewModel getModel() {
    return (FormsTableViewModel) super.getModel();
  }

  private static TableCellRenderer cellWithButton() {
    return (table, value, isSelected, hasFocus, row, column) -> {
      JButton button = (JButton) value;
      button.setOpaque(true);
      button.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
      return button;
    };
  }

  private static <T extends TableModel> TableRowSorter<T> sortBy(T model, int col, SortOrder order) {
    TableRowSorter<T> sorter = new TableRowSorter<>(model);
    sorter.setSortsOnUpdates(true);
    sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(col, order)));
    return sorter;
  }
}
