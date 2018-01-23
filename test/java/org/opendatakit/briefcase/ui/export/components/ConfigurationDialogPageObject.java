package org.opendatakit.briefcase.ui.export.components;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.opendatakit.briefcase.ui.SwingTestRig.uncheckedSleep;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.opendatakit.briefcase.export.ExportConfiguration;

class ConfigurationDialogPageObject {
  private final ConfigurationDialog component;
  private final DialogFixture fixture;

  private ConfigurationDialogPageObject(ConfigurationDialog component, DialogFixture window) {
    this.component = component;
    this.fixture = window;
  }

  static ConfigurationDialogPageObject setUp(Robot robot, ExportConfiguration configuration) {
    ConfigurationDialog dialog = execute(() -> ConfigurationDialog.from(Optional.ofNullable(configuration)));
    DialogFixture fixture = new DialogFixture(robot, dialog.form);
    return new ConfigurationDialogPageObject(dialog, fixture);
  }

  void show() {
    fixture.show();
  }

  public ConfigurationDialogForm dialog() {
    return component.form;
  }

  public JButton clearAllButton() {
    return component.form.clearAllButton;
  }

  public void clickOnOk() {
    click(component.form.okButton);
    uncheckedSleep(50);
  }

  public void clickOnRemove() {
    click(component.form.clearAllButton);
    uncheckedSleep(50);
  }

  public void clickOnCancel() {
    click(component.form.cancelButton);
    uncheckedSleep(50);
  }

  private void click(JButton button) {
    GuiActionRunner.execute(() -> Arrays.asList(button.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(button, 1, ""))));
  }
}
