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
import static org.opendatakit.briefcase.ui.reused.TableCustomizer.customizeHeader;
import static org.opendatakit.briefcase.ui.reused.TableCustomizer.customizeTable;
import static org.opendatakit.briefcase.ui.reused.TableCustomizer.getHeaderDimension;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.opendatakit.briefcase.ui.reused.DetailsButtonComparator;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.SelectionComparator;
import org.opendatakit.briefcase.ui.reused.UI;

public class TransferFormsTableView extends JTable {
  static final Class[] TYPES = new Class[]{Boolean.class, String.class, String.class, JButton.class};
  static final boolean[] EDITABLE_COLS = new boolean[]{true, false, false, false};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int FORM_NAME_COL = 1;
  static final int STATUS_COL = 2;
  static final int DETAIL_BUTTON_COL = 3;
  private final String[] headers;

  TransferFormsTableView(TransferFormsTableViewModel model, String[] headers) {
    super(model);
    this.headers = headers;
    setName("forms");

    addMouseListener(new MouseAdapterBuilder().onClick(this::relayClickToButton).build());

    Dimension formNameDims = getHeaderDimension(this, getHeader(FORM_NAME_COL));
    Dimension statusDims = getHeaderDimension(this, getHeader(STATUS_COL));

    customizeHeader(getTableHeader());
    customizeTable(this);

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
    sorter.setComparator(SELECTED_CHECKBOX_COL, new SelectionComparator());
    sorter.setComparator(DETAIL_BUTTON_COL, new DetailsButtonComparator());
    setRowSorter(sorter);
    sorter.sort();
  }

  static String[] buildHeaders(String actionName) {
    return new String[]{"", "Form Name", actionName + " Status", ""};
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

  private String getHeader(int column) {
    return headers[column];
  }
}
