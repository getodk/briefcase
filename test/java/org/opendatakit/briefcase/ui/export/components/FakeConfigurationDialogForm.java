package org.opendatakit.briefcase.ui.export.components;

import java.awt.event.ActionEvent;
import java.util.Arrays;

class FakeConfigurationDialogForm extends ConfigurationDialogForm {
  boolean okEnabled;
  boolean removeEnabled;

  FakeConfigurationDialogForm(ConfigurationPanelForm form) {
    super(form);
  }

  void clickOK() {
    Arrays.asList(okButton.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(this, 1, "something")));
  }

  void clickRemove() {
    Arrays.asList(clearAllButton.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(this, 1, "something")));
  }

  @Override
  public void enableOK() {
    okEnabled = true;
  }

  @Override
  public void disableOK() {
    okEnabled = false;
  }

  @Override
  public void enableRemove() {
    removeEnabled = true;
  }
}
