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
package org.opendatakit.briefcase.ui.reused;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;

public class TableCustomizer {

  /**
   * for header customization.
   *
   * @param header
   */
  public static void customizeHeader(JTableHeader header) {
    // to make the background color is available.
    header.setOpaque(false);
    header.setBackground(UIManager.getColor("ToolBar.background"));
    header.setBorder(UIManager.getBorder("MenuBar.border"));
  }

  /**
   * for table customization.
   *
   * @param table
   */
  public static void customizeTable(JTable table) {
    table.setGridColor(Color.lightGray);
    table.setRowHeight(28);
  }

  /**
   * to get a dimension from specific header.
   *
   * @param table
   * @param header
   */
  public static Dimension getHeaderDimension(String header, JTable table) {
    return table.getTableHeader()
        .getDefaultRenderer()
        .getTableCellRendererComponent(null, header, false, false, 0, 0)
        .getPreferredSize();
  }
}

