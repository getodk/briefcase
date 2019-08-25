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
package org.opendatakit.briefcase.delivery.ui.export.components;

import static javax.swing.SwingUtilities.invokeLater;
import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.copy;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.WaitTimedOutError;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportConfigurationTest;
import org.opendatakit.briefcase.reused.api.TriStateBoolean;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;

class ConfigurationPanelPageObject {
  static final Path TEST_FOLDER;
  static final Path VALID_PEM_FILE;
  private final ConfigurationPanel component;
  private final FrameFixture fixture;

  static {
    try {
      TEST_FOLDER = Files.createTempDirectory("briefcase_test");
      VALID_PEM_FILE = TEST_FOLDER.resolve("pkey.pem");
      URI sourcePemFileUri = ExportConfigurationTest.class.getClassLoader()
          .getResource("org/opendatakit/briefcase/export/encrypted-form-key.pem")
          .toURI();
      copy(Paths.get(sourcePemFileUri), VALID_PEM_FILE);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private ExportConfiguration currentConf;

  private ConfigurationPanelPageObject(ConfigurationPanel component, FrameFixture fixture) {
    this.component = component;
    this.fixture = fixture;

    component.onChange(conf -> currentConf = conf);
  }

  static ConfigurationPanelPageObject setUp(Robot robot, ExportConfiguration initialConfiguration, boolean isOverridePanel, boolean hasTransferSettings, boolean savePasswordsConsent) {
    return isOverridePanel
        ? setUpOverridePanel(robot, initialConfiguration, savePasswordsConsent, hasTransferSettings)
        : setUpDefaultPanel(robot, initialConfiguration, savePasswordsConsent, hasTransferSettings);
  }


  static ConfigurationPanelPageObject setUpDefaultPanel(Robot robot, ExportConfiguration initialConfiguration, boolean savePasswordsConsent, boolean hasTransferSettings) {
    return execute(() -> {
      ConfigurationPanel configurationPanel = ConfigurationPanel.defaultPanel(initialConfiguration, savePasswordsConsent);
      JFrame testFrame = new JFrame();
      testFrame.add(configurationPanel.getForm().getContainer());
      FrameFixture window = new FrameFixture(robot, testFrame);
      return new ConfigurationPanelPageObject(configurationPanel, window);
    });
  }

  static ConfigurationPanelPageObject setUpOverridePanel(Robot robot, ExportConfiguration initialConfiguration, boolean savePasswordsConsent, boolean hasTransferSettings) {
    return execute(() -> {
      ConfigurationPanel configurationPanel = ConfigurationPanel.overridePanel(initialConfiguration, savePasswordsConsent, hasTransferSettings);
      JFrame testFrame = new JFrame();
      testFrame.add(configurationPanel.getForm().getContainer());
      FrameFixture window = new FrameFixture(robot, testFrame);
      return new ConfigurationPanelPageObject(configurationPanel, window);
    });
  }

  void show() {
    fixture.show();
  }

  void disable(boolean savePasswordsConsent) {
    GuiActionRunner.execute(() -> component.setEnabled(false, savePasswordsConsent));
  }

  void enable(boolean savePasswordsConsent) {
    GuiActionRunner.execute(() -> component.setEnabled(true, savePasswordsConsent));
  }

  public JButton choosePemFileButton() {
    return component.getForm().pemFileChooseButton;
  }

  public JButton chooseExportDirButton() {
    return component.getForm().exportDirChooseButton;
  }

  public JButton clearPemFileButton() {
    return component.getForm().pemFileClearButton;
  }

  public JTextComponent exportDirField() {
    return component.getForm().exportDirField;
  }

  public JTextComponent pemFileField() {
    return component.getForm().pemFileField;
  }

  public DatePicker startDateField() {
    return component.getForm().startDatePicker;
  }

  public DatePicker endDateField() {
    return component.getForm().endDatePicker;
  }

  public JCheckBox pullBeforeField() {
    return component.getForm().pullBeforeField;
  }

  public JLabel pullBeforeOverrideLabel() {
    return component.getForm().pullBeforeOverrideLabel;
  }

  public CustomConfBooleanForm pullBeforeOverrideField() {
    return component.getForm().pullBeforeOverrideField;
  }

  public JTextPane pullBeforeHintPanel() {
    return component.getForm().pullBeforeHintPanel;
  }

  public void setSomePemFile() {
    execute(() -> component.getForm().setPemFile(VALID_PEM_FILE));
  }

  public void setSomeExportDir() {
    Path exportDir = TEST_FOLDER.resolve("some_dir");
    UncheckedFiles.createDirectories(exportDir);
    execute(() -> component.getForm().setExportDir(exportDir));
  }

  public void setStartDate(LocalDate someDate) {
    execute(() -> component.getForm().setStartDate(someDate));
  }

  public void setSomeStartDate() {
    execute(() -> component.getForm().setStartDate(LocalDate.of(2018, 2, 1)));
  }

  public void setEndDate(LocalDate someDate) {
    execute(() -> component.getForm().setEndDate(someDate));
  }

  public void setSomeEndDate() {
    execute(() -> component.getForm().setEndDate(LocalDate.of(2018, 3, 1)));
  }

  public void setPullBefore(boolean value) {
    execute(() -> component.getForm().setPullBefore(value));
  }

  public void setPullBeforeOverride(TriStateBoolean option) {
    execute(() -> component.getForm().setPullBefore(option));
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
    click(component.getForm().exportDirChooseButton);
    uncheckedSleep(50);
  }

  public void clickChoosePemFileButton() {
    click(component.getForm().pemFileChooseButton);
    uncheckedSleep(50);
  }

  public void clickClearPemFileButton() {
    click(component.getForm().pemFileClearButton);
    uncheckedSleep(50);
  }

  public ExportConfiguration getConfiguration() {
    return currentConf;
  }

  public void onChange(Consumer<ExportConfiguration> callback) {
    component.onChange(callback);
  }

  private void click(JButton button) {
    invokeLater(() -> Arrays.asList(button.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(button, 1, ""))));
  }
}
