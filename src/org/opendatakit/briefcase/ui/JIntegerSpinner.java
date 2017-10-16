package org.opendatakit.briefcase.ui;

import java.text.DecimalFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

//Sources:
//https://stackoverflow.com/questions/6449350/make-jspinner-completely-numeric
public class JIntegerSpinner extends JSpinner {

  public JIntegerSpinner(int value, int min, int max, int step) {
    super(new SpinnerNumberModel(8080, 0, 65535, 1));
    JFormattedTextField txt = ((JSpinner.NumberEditor) this.getEditor()).getTextField();
    NumberFormatter formatter = (NumberFormatter) txt.getFormatter();
    formatter.setFormat(new DecimalFormat("#####"));
    formatter.setAllowsInvalid(false);
    ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
    txt.setValue(value);
  }
}

