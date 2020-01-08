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

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;

import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;
import org.opendatakit.briefcase.model.FormStatus;

public class DetailsStatusButton implements ButtonProcessing, Comparable<ButtonProcessing> {
  private static final Font IC_RECEIPT = FontUtils.getCustomFont("ic_receipt.ttf", 16f);

  private final JButton button;

  private DetailsStatusButton(JButton button) {
    this.button = button;
  }

  public static DetailsStatusButton buildDetailButton(FormStatus form) {
    // Use custom fonts instead of png for easier scaling
    DetailsStatusButton button = DetailsStatusButton.create();
    button.onClick(() -> {
      if (!form.getStatusHistory().isEmpty())
        showDialog(getFrameForComponent(button.getJButton()), form.getFormDefinition(), form.getStatusHistory());
    });
    return button;
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  private static DetailsStatusButton create() {
    JButton button = new JButton();
    button.setText("\uE900");
    button.setFont(IC_RECEIPT);
    button.setToolTipText("View this form's status history");
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setForeground(LIGHT_GRAY);
    return new DetailsStatusButton(button);
  }

  public JButton getJButton() {
    return button;
  }

  public void setStatus(boolean value) {
    button.setForeground(value ? DARK_GRAY : LIGHT_GRAY);
  }

  public int compareTo(ButtonProcessing b) {
    boolean aStatus = this.getJButton().getForeground().equals(DARK_GRAY);
    boolean bStatus = b.getJButton().getForeground().equals(DARK_GRAY);
    if (aStatus == bStatus)
      return 0;
    if (aStatus)
      return -1;
    return 1;
  }

  public void onClick(Runnable callback) {
    button.addActionListener(__ -> callback.run());
  }

  public void setEnabled(boolean enabled) {
    button.setEnabled(enabled);
  }
}
