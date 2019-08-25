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

package org.opendatakit.briefcase.delivery.ui.settings;

import java.text.DecimalFormat;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

//Sources:
//https://stackoverflow.com/questions/6449350/make-jspinner-completely-numeric
class JIntegerSpinner extends JSpinner {
  JIntegerSpinner(int value, int min, int max, int step) {
    super(new SpinnerNumberModel(value, min, max, step));
    JFormattedTextField txt = ((JSpinner.NumberEditor) this.getEditor()).getTextField();
    NumberFormatter formatter = (NumberFormatter) txt.getFormatter();
    formatter.setFormat(new DecimalFormat("#####"));
    formatter.setAllowsInvalid(false);
    ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
    txt.setValue(value);
  }
}

