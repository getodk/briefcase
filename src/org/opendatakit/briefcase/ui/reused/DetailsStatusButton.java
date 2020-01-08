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

public class DetailsStatusButton extends JButton implements Comparable<DetailsStatusButton> {
  private static final Font IC_RECEIPT = FontUtils.getCustomFont("ic_receipt.ttf", 16f);

  private boolean status = false;

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  public DetailsStatusButton() {
    super();
    // Use custom fonts instead of png for easier scaling
    setText("\uE900");
    setFont(IC_RECEIPT);// custom font that overrides  with a receipt icon
    setToolTipText("View this form's status history");
    setMargin(new Insets(0, 0, 0, 0));
    setForeground(LIGHT_GRAY);
  }

  public void setStatus(boolean value) {
    status = value;
    setForeground(status ? DARK_GRAY : LIGHT_GRAY);
  }

  private boolean getStatus() {
    return status;
  }

  public int compareTo(DetailsStatusButton button) {
    if (this.getStatus() == button.getStatus())
      return 0;
    if (this.getStatus() && !button.getStatus())
      return -1;
    return 1;
  }

}
