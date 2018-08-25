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

import static javax.swing.SortOrder.ASCENDING;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.UI;
import org.opendatakit.briefcase.util.FindDirectoryStructure;

public class TransferFormsTableView extends JTable {
  static final Class[] TYPES = new Class[]{Boolean.class, String.class, String.class, JButton.class};
  static final boolean[] EDITABLE_COLS = new boolean[]{true, false, false, false};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int FORM_NAME_COL = 1;
  static final int STATUS_COL = 2;
  static final int DETAIL_BUTTON_COL = 3;
  private final String[] headers;

  public TransferFormsTableView(TransferFormsTableViewModel model, String[] headers) {
    super(model);
    this.headers = headers;
    setName("forms");

    addMouseListener(new MouseAdapterBuilder().onClick(this::relayClickToButton).build());

    Dimension formNameDims = getHeaderDimension(getHeader(FORM_NAME_COL));
    Dimension statusDims = getHeaderDimension(getHeader(STATUS_COL));

    JTableHeader header = getTableHeader();
    if (FindDirectoryStructure.isWindows()) {
      Border border = BorderFactory.createLineBorder(Color.GRAY);
      header.setBackground(Color.GRAY);
      header.setBorder(border);
    }

    setRowHeight(28);

    TableColumnModel columns = getColumnModel();
    columns.getColumn(SELECTED_CHECKBOX_COL).setMinWidth(40);
    columns.getColumn(SELECTED_CHECKBOX_COL).setMaxWidth(40);
    columns.getColumn(SELECTED_CHECKBOX_COL).setPreferredWidth(40);
    columns.getColumn(FORM_NAME_COL).setMinWidth(formNameDims.width + 25);
    columns.getColumn(FORM_NAME_COL).setPreferredWidth(formNameDims.width + 25);
    columns.getColumn(STATUS_COL).setMinWidth(statusDims.width + 25);
    columns.getColumn(STATUS_COL).setPreferredWidth(statusDims.width + 25);
    columns.getColumn(DETAIL_BUTTON_COL).setCellRenderer(UI::cellWithButton);
    columns.getColumn(DETAIL_BUTTON_COL).setMinWidth(40);
    columns.getColumn(DETAIL_BUTTON_COL).setMaxWidth(40);
    columns.getColumn(DETAIL_BUTTON_COL).setPreferredWidth(40);

    setFillsViewportHeight(true);

    TableRowSorter<TransferFormsTableViewModel> sorter = sortBy(getModel(), FORM_NAME_COL, ASCENDING);
    setRowSorter(sorter);
    sorter.sort();
  }

  public static String[] buildHeaders(String actionName) {
    return new String[]{"", "Form Name", actionName + " Status", ""};
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
  public TransferFormsTableViewModel getModel() {
    return (TransferFormsTableViewModel) super.getModel();
  }

  private static <T extends TableModel> TableRowSorter<T> sortBy(T model, int col, SortOrder order) {
    TableRowSorter<T> sorter = new TableRowSorter<>(model);
    sorter.setSortsOnUpdates(true);
    sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(col, order)));
    return sorter;
  }

  public String getHeader(int column) {
    return headers[column];
  }
}
