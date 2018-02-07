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

import static javax.swing.SwingUtilities.invokeLater;
import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Timeout.timeout;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.WaitTimedOutError;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.opendatakit.briefcase.export.ExportConfiguration;

class ConfigurationPanelPageObject {
  private static Path TEST_FOLDER;
  private final ConfigurationPanel component;
  private final FrameFixture fixture;

  static {
    try {
      TEST_FOLDER = Files.createTempDirectory("briefcase_test");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ConfigurationPanelPageObject(ConfigurationPanel component, FrameFixture fixture) {
    this.component = component;
    this.fixture = fixture;
  }

  static ConfigurationPanelPageObject setUp(Robot robot, ExportConfiguration initialConfiguration, boolean isOverridePanel) {
    ConfigurationPanel configurationPanel = execute(() -> isOverridePanel
        ? ConfigurationPanel.overridePanel(initialConfiguration, true, true)
        : ConfigurationPanel.defaultPanel(initialConfiguration, true));
    JFrame testFrame = execute(() -> {
      JFrame f = new JFrame();
      f.add(configurationPanel.getForm().container);
      return f;
    });
    FrameFixture window = new FrameFixture(robot, testFrame);
    return new ConfigurationPanelPageObject(configurationPanel, window);
  }

  void show() {
    fixture.show();
  }

  void disable() {
    GuiActionRunner.execute(component::disable);
  }

  void enable() {
    GuiActionRunner.execute(component::enable);
  }

  public JButton choosePemFileButton() {
    return component.form.pemFileChooseButton;
  }

  public JButton chooseExportDirButton() {
    return component.form.exportDirChooseButton;
  }

  public JButton clearPemFileButton() {
    return component.form.pemFileClearButton;
  }

  public JTextComponent exportDirField() {
    return component.form.exportDirField;
  }

  public JTextComponent pemFileField() {
    return component.form.pemFileField;
  }

  public DatePicker startDateField() {
    return component.form.startDatePicker;
  }

  public DatePicker endDateField() {
    return component.form.endDatePicker;
  }

  public JCheckBox pullBeforeField() {
    return component.form.pullBeforeField;
  }

  public JCheckBox pullBeforeInheritField() {
    return component.form.pullBeforeInheritField;
  }

  public void setSomePemFile() {
    Path pemFile = TEST_FOLDER.resolve("some_file.pem");
    execute(() -> component.form.setPemFile(pemFile));
  }

  public void setSomeExportDir() {
    Path exportDir = TEST_FOLDER.resolve("some_dir");
    execute(() -> component.form.setExportDir(exportDir));
  }

  public void setStartDate(LocalDate someDate) {
    // GUI actions launched with GuiActionRunner.execute(...) will block the thread
    // until their completion. This is problematic when the action launches a modal
    // dialog, which blocks the EDT, preventing us to make any assertion on the GUI
    // status. Using SwingUtilities.invokeLater(...) solves this issue but forces
    // us to manage some wait times between actions involving appearing/disappearing
    // dialogs.
    invokeLater(() -> component.form.setStartDate(someDate));
    uncheckedSleep(50);
  }

  public void setSomeStartDate() {
    execute(() -> component.form.setStartDate(LocalDate.of(2018, 2, 1)));
  }

  public void setEndDate(LocalDate someDate) {
    // GUI actions launched with GuiActionRunner.execute(...) will block the thread
    // until their completion. This is problematic when the action launches a modal
    // dialog, which blocks the EDT, preventing us to make any assertion on the GUI
    // status. Using SwingUtilities.invokeLater(...) solves this issue but forces
    // us to manage some wait times between actions involving appearing/disappearing
    // dialogs.
    invokeLater(() -> component.form.setEndDate(someDate));
    uncheckedSleep(50);
  }

  public void setSomeEndDate() {
    execute(() -> component.form.setEndDate(LocalDate.of(2018, 3, 1)));
  }

  public void setPullBefore(boolean value) {
    invokeLater(() -> {
      JCheckBox field = component.form.pullBeforeField;
      field.setSelected(value);
      Arrays.asList(field.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(field, 1, "")));
    });
    uncheckedSleep(50);
  }

  public void setPullBeforeInherit(boolean value) {
    invokeLater(() -> {
      JCheckBox field = component.form.pullBeforeInheritField;
      field.setSelected(value);
      Arrays.asList(field.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(field, 1, "")));
    });
    uncheckedSleep(50);
  }

  private JFileChooserFixture fileDialog() {
    return fileDialog(50);
  }

  JFileChooserFixture fileDialog(int timeoutMillis) {
    // Similar to buttonByName(name) or textFieldByName(name), we won't
    // throw when exhausting the timeout for obtaining a dialog and we will
    // return a null instead, which is more suitable for our SwingMatchers
    // and for expressing intent in our tests
    try {
      return fixture.fileChooser(timeout(timeoutMillis));
    } catch (WaitTimedOutError e) {
      return null;
    }
  }

  public void cancelFileDialog() {
    // We need to be sure that the dialog is rendered before trying to cancel it
    waitFor(() -> fileDialog(500) != null);
    fileDialog().cancel();
    // We wait for the dialog to disappear before handing over the thread
    waitFor(() -> fileDialog() == null);
  }

  private void waitFor(Supplier<Boolean> condition) {
    // By default, wait 100 millis and then start again while the
    // condition returns false.
    // Implicitly, we're going to use this method while waiting for some
    // GUI element to be ready and waiting 100 millis by default is safer
    // (in my experience)
    do {
      uncheckedSleep(100);
    } while (!condition.get());
  }

  private void uncheckedSleep(int millis) {
    // Just to avoid boilerplate try/catch block
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void clickChooseExportDirButton() {
    click(component.form.exportDirChooseButton);
    uncheckedSleep(50);
  }

  public void clickChoosePemFileButton() {
    click(component.form.pemFileChooseButton);
    uncheckedSleep(50);
  }

  public void clickClearPemFileButton() {
    click(component.form.pemFileClearButton);
    uncheckedSleep(50);
  }

  public ExportConfiguration getConfiguration() {
    return component.getConfiguration();
  }

  public void onChange(Runnable callback) {
    component.onChange(callback);
  }

  private void click(JButton button) {
    SwingUtilities.invokeLater(() -> Arrays.asList(button.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(button, 1, ""))));
  }
}
