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
package org.opendatakit.briefcase.ui.export.components;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.opendatakit.briefcase.ui.SwingTestRig.uncheckedSleep;

import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.Arrays;
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
    ConfigurationDialog dialog = execute(() -> ConfigurationDialog.from(configuration, true, true));
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

  public JButton okButton() {
    return component.form.okButton;
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

  public void setSomeExportDir() {
    GuiActionRunner.execute(() -> component.getConfPanel().form.setExportDir(Paths.get("/some/dir")));
  }

  public void clearExportDir() {
    GuiActionRunner.execute(() -> component.getConfPanel().form.clearExportDir());
  }

  public void onOK(Runnable callback) {
    component.onOK(__ -> callback.run());
  }

  public void onRemove(Runnable callback) {
    component.onRemove(callback::run);
  }

  private void click(JButton button) {
    GuiActionRunner.execute(() -> Arrays.asList(button.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(button, 1, ""))));
  }
}
