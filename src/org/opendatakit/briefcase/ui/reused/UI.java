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

import static java.awt.Color.LIGHT_GRAY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;

import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import org.opendatakit.briefcase.model.FormStatus;

public class UI {
  private static final Font ic_receipt = FontUtils.getCustomFont("ic_receipt.ttf", 16f);

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  public static JButton buildDetailButton(FormStatus form) {
    // Use custom fonts instead of png for easier scaling
    JButton button = new JButton("\uE900");
    button.setFont(ic_receipt); // custom font that overrides  with a receipt icon
    button.setToolTipText("View this form's status history");
    button.setMargin(new Insets(0, 0, 0, 0));

    button.setForeground(LIGHT_GRAY);
    button.addActionListener(__ -> {
      if (!form.getStatusHistory().isEmpty())
        showDialog(getFrameForComponent(button), form.getFormDefinition(), form.getStatusHistory());
    });
    return button;
  }

  public static JButton cellWithButton(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    JButton button = (JButton) value;
    button.setOpaque(true);
    button.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    return button;
  }

  public static void errorMessage(String title, String message) {
    errorMessage(title, message, false);
  }

  public static void errorMessage(String title, String message, boolean nonBlocking) {
    JDialog dialog = new JDialog();
    dialog.setAlwaysOnTop(true);
    if (nonBlocking) {
      new SwingWorker() {
        @Override
        protected Object doInBackground() {
          JOptionPane.showMessageDialog(dialog, message, title, ERROR_MESSAGE);
          return null;
        }
      }.execute();
    } else {
      JOptionPane.showMessageDialog(dialog, message, title, ERROR_MESSAGE);
    }
  }
}
