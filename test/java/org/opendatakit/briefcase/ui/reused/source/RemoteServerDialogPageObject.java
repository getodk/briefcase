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
package org.opendatakit.briefcase.ui.reused.source;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.opendatakit.briefcase.ui.SwingTestRig.uncheckedSleep;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.timing.Timeout;
import org.opendatakit.briefcase.reused.RemoteServer;

class RemoteServerDialogPageObject {
  final RemoteServerDialog component;
  private final DialogFixture fixture;

  private RemoteServerDialogPageObject(RemoteServerDialog component, DialogFixture window) {
    this.component = component;
    this.fixture = window;
  }

  static RemoteServerDialogPageObject setUp(Robot robot, RemoteServer.Test serverTester) {
    RemoteServerDialog dialog = execute(() -> RemoteServerDialog.empty(serverTester, "Form Manager"));
    DialogFixture fixture = new DialogFixture(robot, dialog.form);
    return new RemoteServerDialogPageObject(dialog, fixture);
  }

  void show() {
    fixture.show();
  }

  public RemoteServerDialogForm dialog() {
    return component.form;
  }

  public void clickOnConnect() {
    click(component.form.connectButton);
    uncheckedSleep(100);
  }

  private void click(JButton button) {
    GuiActionRunner.execute(() -> Arrays.asList(button.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(button, 1, ""))));
  }

  public void fillForm(String url) {
    GuiActionRunner.execute(() -> component.form.urlField.setText(url));
  }

  public JButton connectButton() {
    return component.form.connectButton;
  }

  public JButton cancelButton() {
    return component.form.cancelButton;
  }

  public JTextField urlField() {
    return component.form.urlField;
  }

  public JTextField usernameField() {
    return component.form.usernameField;
  }

  public JTextField passwordField() {
    return component.form.passwordField;
  }

  public JProgressBar progressBar() {
    return component.form.progressBar;
  }

  public JOptionPane errorDialog() {
    JOptionPaneFixture jOptionPaneFixture = fixture.optionPane(Timeout.timeout(250));
    return jOptionPaneFixture.target();
  }
}
