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

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.JButton;


/**
 * Contains application logic pertaining to the Export Configuration Button
 */
public class ExportConfigurationButton {
  private static final Color NO_CONF_OVERRIDE_COLOR = new Color(0, 128, 0);
  private static final Font IC_SETTINGS = FontUtils.getCustomFont("ic_settings.ttf", 16f);

  private final JButton button;

  private ExportConfigurationButton(JButton button) {
    this.button = button;
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  public static ExportConfigurationButton create() {
    JButton button = new JButton();
    button.setText("\uE900");
    button.setFont(IC_SETTINGS);
    button.setToolTipText("Override the export configuration for this form");
    button.setMargin(new Insets(0, 0, 0, 0));
    return new ExportConfigurationButton(button);
  }

  public JButton getJButton() {
    return button;
  }

  public void setConfigured(boolean configured) {
    button.setForeground(configured ? NO_CONF_OVERRIDE_COLOR : DARK_GRAY);
  }

  public static int compare(ExportConfigurationButton a, ExportConfigurationButton b) {
    boolean aConfigured = a.getJButton().getForeground().equals(NO_CONF_OVERRIDE_COLOR);
    boolean bConfigured = b.getJButton().getForeground().equals(NO_CONF_OVERRIDE_COLOR);
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
