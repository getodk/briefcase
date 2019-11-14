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
import java.util.Comparator;
import javax.swing.JButton;

public class ConfigurationButtonComparator  implements Comparator<JButton> {
  private static final Color NO_CONF_OVERRIDE_COLOR = new Color(0, 128, 0);

  public int compare(JButton o1, JButton o2) {
    int result;
    if (o1.getForeground().equals(o2.getForeground())) result = 0;
    else if (o1.getForeground().equals(NO_CONF_OVERRIDE_COLOR)  && o2.getForeground().equals(DARK_GRAY)) result = -1;
    else result = 1;
    return result;
  }
}