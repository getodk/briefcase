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
package org.opendatakit.briefcase.delivery.ui.reused;

import static java.awt.Color.lightGray;
import static javax.swing.UIManager.getBorder;
import static javax.swing.UIManager.getColor;

import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

public class TableCustomizer {

  /**
   * Sets properties of the given {@link JTableHeader} to improve its
   * visual appearance across all platforms (Mac, Windows, and Linux).
   */
  public static void customizeHeader(JTableHeader header) {
    header.setOpaque(false); // to make the background color is available.
    header.setBackground(getColor("ToolBar.background"));
    header.setBorder(getBorder("MenuBar.border"));
  }

  /**
   * Sets properties of the given {@link JTable} to improve the visual
   * appearance of table across all platforms (Mac, Windows, and Linux).
   */
  public static void customizeTable(JTable table) {
    table.setGridColor(lightGray);
    table.setRowHeight(28);
  }

  /**
   * Returns the {@link Dimension} of the given header in the given {@link JTable}
   * using its related cell renderer component's preferred size.
   */
  public static Dimension getHeaderDimension(JTable table, String header) {
    return table.getTableHeader()
        .getDefaultRenderer()
        .getTableCellRendererComponent(null, header, false, false, 0, 0)
        .getPreferredSize();
  }
}

