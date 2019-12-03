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
public class ExportConfigurationButton extends JButton implements Comparable<ExportConfigurationButton> {
  private static final Color NO_CONF_OVERRIDE_COLOR = new Color(0, 128, 0);
  private static final Font IC_SETTINGS = FontUtils.getCustomFont("ic_settings.ttf", 16f);

  private boolean configured = false;

  public ExportConfigurationButton() {
    super();
    // Use custom fonts instead of png for easier scaling
    setText("\uE900");
    setFont(IC_SETTINGS);
    setToolTipText("Override the export configuration for this form");
    setMargin(new Insets(0, 0, 0, 0));
  }

  public void setConfigured(boolean value) {
    configured = value;
    setForeground(configured ? NO_CONF_OVERRIDE_COLOR : DARK_GRAY);
  }

  public boolean isConfigured() {
    return configured;
  }

  public int compareTo(ExportConfigurationButton button) {
    if (this.isConfigured() == button.isConfigured())
      return 0;
    if (this.isConfigured() && !button.isConfigured())
      return -1;
    return 1;
  }

}
