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

import javax.swing.JButton;
import javax.swing.JTable;

/**
 * Defines common behavior used in buttons
 */
public interface ButtonProcessing {

  static JButton cellWithButton(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    JButton button = ((ButtonProcessing) value).getJButton();
    button.setOpaque(true);
    button.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    return button;
  }

  void onClick(Runnable callback);

  JButton getJButton();

}
