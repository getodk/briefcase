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

import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;

public class DetailsStatusButton {
  private static final Font IC_RECEIPT = FontUtils.getCustomFont("ic_receipt.ttf", 16f);

  private boolean status = false;
  private final JButton button;

  private DetailsStatusButton(JButton button) {
    this.button = button;
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  public static DetailsStatusButton create() {
    JButton button = new JButton();
    button.setText("\uE900");
    button.setFont(IC_RECEIPT);
    button.setToolTipText("View this form's status history");
    button.setMargin(new Insets(0, 0, 0, 0));
    return new DetailsStatusButton(button);
  }

  public JButton getJButton() {
    return button;
  }

  public void setStatus(boolean value) {
    status = value;
    button.setForeground(status ? DARK_GRAY : LIGHT_GRAY);
  }

  private boolean getStatus() {
    return status;
  }

  public static int compare(ExportConfigurationButton a, ExportConfigurationButton b) {
    boolean aConfigured = a.getJButton().getForeground().equals(LIGHT_GRAY);
    boolean bConfigured = b.getJButton().getForeground().equals(LIGHT_GRAY);
    if (aConfigured == bConfigured)
      return 0;
    if (aConfigured)
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
