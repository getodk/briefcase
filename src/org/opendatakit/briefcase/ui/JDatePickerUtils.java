package org.opendatakit.briefcase.ui;

import org.jdatepicker.impl.JDatePickerImpl;

public class JDatePickerUtils {
  
  // This capability is only available in 1.3.4.1 release, which isn't in maven (only 1.3.4)
  public static void setEnabled(boolean enabled, JDatePickerImpl ... pickers) {
    for (JDatePickerImpl picker : pickers) {
      // The JButton, no public accessor for it
      picker.getComponent(1).setEnabled(enabled);
      picker.getJFormattedTextField().setEnabled(enabled);
    }
  }
}