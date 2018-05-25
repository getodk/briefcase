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
package org.opendatakit.briefcase.ui.push.components;

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
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;

public class PushFormsTableView extends JTable {
  static final String[] HEADERS = new String[]{"", "Form Name", "Push Status", ""};
  static final Class[] TYPES = new Class[]{Boolean.class, String.class, String.class, JButton.class};
  static final boolean[] EDITABLE_COLS = new boolean[]{true, false, false, false};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int FORM_NAME_COL = 1;
  static final int PUSH_STATUS_COL = 2;
  static final int DETAIL_BUTTON_COL = 3;

  public PushFormsTableView(PushFormsTableViewModel model) {
    super(model);
    setName("forms");

    addMouseListener(new MouseAdapterBuilder().onClick(this::relayClickToButton).build());

    Dimension formNameDims = getHeaderDimension(HEADERS[FORM_NAME_COL]);
    Dimension pushStatusDims = getHeaderDimension(HEADERS[PUSH_STATUS_COL]);

    setRowHeight(28);

    TableColumnModel columns = getColumnModel();
    columns.getColumn(SELECTED_CHECKBOX_COL).setMinWidth(40);
    columns.getColumn(SELECTED_CHECKBOX_COL).setMaxWidth(40);
    columns.getColumn(SELECTED_CHECKBOX_COL).setPreferredWidth(40);
    columns.getColumn(FORM_NAME_COL).setMinWidth(formNameDims.width + 25);
    columns.getColumn(FORM_NAME_COL).setPreferredWidth(formNameDims.width + 25);
    columns.getColumn(PUSH_STATUS_COL).setMinWidth(pushStatusDims.width + 25);
    columns.getColumn(PUSH_STATUS_COL).setPreferredWidth(pushStatusDims.width + 25);
    columns.getColumn(DETAIL_BUTTON_COL).setCellRenderer(cellWithButton());
    columns.getColumn(DETAIL_BUTTON_COL).setMinWidth(40);
    columns.getColumn(DETAIL_BUTTON_COL).setMaxWidth(40);
    columns.getColumn(DETAIL_BUTTON_COL).setPreferredWidth(40);

    setFillsViewportHeight(true);

    TableRowSorter<PushFormsTableViewModel> sorter = sortBy(getModel(), FORM_NAME_COL, ASCENDING);
    setRowSorter(sorter);
    sorter.sort();
  }

  private Dimension getHeaderDimension(String header) {
    return getTableHeader()
        .getDefaultRenderer()
        .getTableCellRendererComponent(null, header, false, false, 0, 0)
        .getPreferredSize();
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
  public PushFormsTableViewModel getModel() {
    return (PushFormsTableViewModel) super.getModel();
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
