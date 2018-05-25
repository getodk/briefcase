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

import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static org.opendatakit.briefcase.export.PullBeforeOverrideOption.INHERIT;
import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;
import static org.opendatakit.briefcase.ui.reused.FileChooser.file;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.isMac;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.isWindows;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import org.opendatakit.briefcase.export.PullBeforeOverrideOption;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.StringUtils;

@SuppressWarnings("checkstyle:MethodName")
public class ConfigurationPanelForm extends JComponent {
  public JPanel container;
  protected final DatePicker startDatePicker;
  protected final DatePicker endDatePicker;
  protected JTextField exportDirField;
  protected JTextField pemFileField;
  protected JButton exportDirChooseButton;
  private JLabel exportDirLabel;
  private JLabel pemFileLabel;
  private JLabel startDateLabel;
  private JLabel endDateLabel;
  private JPanel pemFileButtons;
  protected JButton pemFileChooseButton;
  protected JButton pemFileClearButton;
  private JPanel exportDirButtons;
  private JButton exportDirCleanButton;
  JCheckBox pullBeforeField;
  JComboBox<PullBeforeOverrideOption> pullBeforeOverrideField;
  JTextPane pullBeforeHintPanel;
  JLabel pullBeforeOverrideLabel;
  private JCheckBox overwriteFilesField;
  private final List<Consumer<Path>> onSelectExportDirCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onSelectPemFileCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectStartDateCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectEndDateCallbacks = new ArrayList<>();
  private final List<Consumer<Boolean>> onChangePullBeforeCallbacks = new ArrayList<>();
  private final List<Consumer<PullBeforeOverrideOption>> onChangePullBeforeOverrideCallbacks = new ArrayList<>();
  private final List<Consumer<Boolean>> onChangeOverwriteExistingFilesCallbacks = new ArrayList<>();
  private final ConfigurationPanelMode mode;

  protected ConfigurationPanelForm(ConfigurationPanelMode mode) {
    this.mode = mode;
    startDatePicker = createDatePicker();
    endDatePicker = createDatePicker();
    pullBeforeOverrideField = new JComboBox<>(PullBeforeOverrideOption.values());
    pullBeforeOverrideField.setSelectedItem(INHERIT);
    $$$setupUI$$$();
    startDatePicker.getSettings().setGapBeforeButtonPixels(0);
    startDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    startDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());
    endDatePicker.getSettings().setGapBeforeButtonPixels(0);
    endDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    endDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());
    pullBeforeHintPanel.setBackground(new Color(255, 255, 255, 0));
    mode.decorate(pullBeforeField, pullBeforeOverrideLabel, pullBeforeOverrideField, pullBeforeHintPanel);
    GridBagLayout layout = (GridBagLayout) container.getLayout();
    GridBagConstraints constraints = layout.getConstraints(pullBeforeHintPanel);
    constraints.insets = new Insets(0, isMac() ? 6 : isWindows() ? 2 : 0, 0, 0);
    layout.setConstraints(pullBeforeHintPanel, constraints);

    exportDirChooseButton.addActionListener(__ ->
        buildExportDirDialog().choose().ifPresent(file -> setExportDir(Paths.get(file.toURI())))
    );
    exportDirCleanButton.addActionListener(__ -> clearExportDir());
    pemFileChooseButton.addActionListener(__ ->
        buildPemFileDialog().choose().ifPresent(file -> setPemFile(Paths.get(file.toURI())))
    );
    pemFileClearButton.addActionListener(__ -> clearPemFile());

    startDatePicker.addDateChangeListener(event -> {
      endDatePicker.getSettings().setDateRangeLimits(event.getNewDate(), null);
      onSelectStartDateCallbacks.forEach(consumer -> consumer.accept(event.getNewDate()));
    });

    endDatePicker.addDateChangeListener(event -> {
      startDatePicker.getSettings().setDateRangeLimits(null, event.getNewDate());
      onSelectEndDateCallbacks.forEach(consumer -> consumer.accept(event.getNewDate()));
    });
    pullBeforeField.addActionListener(__ -> triggerChangePullBefore());
    pullBeforeOverrideField.addActionListener(__ -> triggerChangePullBeforeOverride());
    overwriteFilesField.addActionListener(__ -> {
      if (!overwriteFilesField.isSelected() || confirmOverwriteFiles())
        triggerOverwriteExistingFiles();
    });
  }

  public static ConfigurationPanelForm overridePanel(boolean savePasswordsConsent, boolean hasTransferSettings) {
    return new ConfigurationPanelForm(ConfigurationPanelMode.overridePanel(savePasswordsConsent, hasTransferSettings));
  }

  public static ConfigurationPanelForm defaultPanel(boolean savePasswordsConsent, boolean hasTransferSettings) {
    return new ConfigurationPanelForm(ConfigurationPanelMode.defaultPanel(savePasswordsConsent, hasTransferSettings));
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      for (Component c : container.getComponents())
        c.setEnabled(true);
      container.setEnabled(true);
    } else {
      for (Component c : container.getComponents())
        c.setEnabled(false);
      container.setEnabled(false);
    }
  }

  public void setExportDir(Path path) {
    exportDirField.setText(path.toString());
    onSelectExportDirCallbacks.forEach(consumer -> consumer.accept(path));
    if (mode.isExportDirCleanable()) {
      exportDirChooseButton.setVisible(false);
      exportDirCleanButton.setVisible(true);
    }
  }

  void clearExportDir() {
    exportDirField.setText(null);
    onSelectExportDirCallbacks.forEach(consumer -> consumer.accept(null));
    if (mode.isExportDirCleanable()) {
      exportDirChooseButton.setVisible(true);
      exportDirCleanButton.setVisible(false);
    }
  }

  void setPemFile(Path path) {
    pemFileField.setText(path.toString());
    onSelectPemFileCallbacks.forEach(consumer -> consumer.accept(path));
    pemFileChooseButton.setVisible(false);
    pemFileClearButton.setVisible(true);
  }

  private void clearPemFile() {
    pemFileField.setText(null);
    onSelectPemFileCallbacks.forEach(consumer -> consumer.accept(null));
    pemFileChooseButton.setVisible(true);
    pemFileClearButton.setVisible(false);
  }

  void setStartDate(LocalDate date) {
    // Route the change through the date picker's date to avoid repeated set calls
    startDatePicker.setDate(LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void setEndDate(LocalDate date) {
    // Route the change through the date picker's date to avoid repeated set calls
    endDatePicker.setDate(LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void setPullBefore(boolean value) {
    pullBeforeField.setSelected(value);
  }

  void setPullBeforeOverride(PullBeforeOverrideOption value) {
    pullBeforeOverrideField.setSelectedItem(value);
  }

  void setOverwriteExistingFiles(boolean value) {
    overwriteFilesField.setSelected(value);
  }

  void onSelectExportDir(Consumer<Path> callback) {
    onSelectExportDirCallbacks.add(callback);
  }

  void onSelectPemFile(Consumer<Path> callback) {
    onSelectPemFileCallbacks.add(callback);
  }

  void onSelectDateRangeStart(Consumer<LocalDate> callback) {
    onSelectStartDateCallbacks.add(callback);
  }

  void onSelectDateRangeEnd(Consumer<LocalDate> callback) {
    onSelectEndDateCallbacks.add(callback);
  }

  void onChangePullBefore(Consumer<Boolean> callback) {
    onChangePullBeforeCallbacks.add(callback);
  }

  void onChangePullBeforeOverride(Consumer<PullBeforeOverrideOption> callback) {
    onChangePullBeforeOverrideCallbacks.add(callback);
  }

  void onChangeOverwriteExistingFiles(Consumer<Boolean> callback) {
    onChangeOverwriteExistingFilesCallbacks.add(callback);
  }

  void changeMode(boolean savePasswordsConsent) {
    mode.setSavePasswordsConsent(savePasswordsConsent);
    mode.decorate(pullBeforeField, pullBeforeOverrideLabel, pullBeforeOverrideField, pullBeforeHintPanel);
  }

  private void createUIComponents() {
    // Custom creation of components occurs inside the constructor
  }

  /**
   * The DatePicker default text box and calendar button don't match with the rest of the UI.
   * This tweaks those elements to be consistent with the rest of the application.
   */
  private DatePicker createDatePicker() {
    JTextField model = new JTextField();

    DatePicker datePicker = new DatePicker();
    datePicker.getComponentDateTextField().setBorder(model.getBorder());
    datePicker.getComponentDateTextField().setMargin(new Insets(0, 0, 0, 0));
    datePicker.getComponentDateTextField().setEditable(false);
    datePicker.getComponentToggleCalendarButton().setText("Choose...");
    datePicker.getComponentToggleCalendarButton().setMargin(new Insets(0, 0, 0, 0));

    return datePicker;
  }

  private FileChooser buildPemFileDialog() {
    return file(container, fileFrom(pemFileField));
  }

  private FileChooser buildExportDirDialog() {
    return directory(
        container,
        fileFrom(exportDirField),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    );
  }

  private static Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::nullOrEmpty)
        .map(path -> Paths.get(path).toFile());
  }

  private void triggerChangePullBefore() {
    onChangePullBeforeCallbacks.forEach(callback -> callback.accept(pullBeforeField.isSelected()));
  }

  private void triggerChangePullBeforeOverride() {
    onChangePullBeforeOverrideCallbacks.forEach(callback -> callback.accept((PullBeforeOverrideOption) pullBeforeOverrideField.getSelectedItem()));
  }


  private void triggerOverwriteExistingFiles() {
    onChangeOverwriteExistingFilesCallbacks.forEach(callback -> callback.accept(overwriteFilesField.isSelected()));
  }


  private boolean confirmOverwriteFiles() {
    if (showConfirmDialog(this, "Overwrite existing files?", "", YES_NO_OPTION, PLAIN_MESSAGE) == YES_OPTION)
      return true;
    overwriteFilesField.setSelected(false);
    return false;
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    container = new JPanel();
    container.setLayout(new GridBagLayout());
    exportDirLabel = new JLabel();
    exportDirLabel.setText("Export directory");
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(exportDirLabel, gbc);
    pemFileLabel = new JLabel();
    pemFileLabel.setText("PEM file location");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(pemFileLabel, gbc);
    startDateLabel = new JLabel();
    startDateLabel.setText("Start date");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(startDateLabel, gbc);
    endDateLabel = new JLabel();
    endDateLabel.setText("End date");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(endDateLabel, gbc);
    exportDirField = new JTextField();
    exportDirField.setEditable(false);
    exportDirField.setName("exportDir");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(exportDirField, gbc);
    pemFileField = new JTextField();
    pemFileField.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(pemFileField, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(startDatePicker, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(endDatePicker, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer4, gbc);
    pullBeforeField = new JCheckBox();
    pullBeforeField.setText("Pull before export");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 5;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(pullBeforeField, gbc);
    pullBeforeHintPanel = new JTextPane();
    pullBeforeHintPanel.setAutoscrolls(false);
    pullBeforeHintPanel.setEditable(false);
    pullBeforeHintPanel.setFocusCycleRoot(false);
    pullBeforeHintPanel.setFocusable(false);
    pullBeforeHintPanel.setOpaque(false);
    pullBeforeHintPanel.setText("Some hint will be shown here");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 7;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(pullBeforeHintPanel, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(pullBeforeOverrideField, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer5, gbc);
    pullBeforeOverrideLabel = new JLabel();
    pullBeforeOverrideLabel.setText("Pull before export");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(pullBeforeOverrideLabel, gbc);
    exportDirButtons = new JPanel();
    exportDirButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(exportDirButtons, gbc);
    exportDirCleanButton = new JButton();
    exportDirCleanButton.setText("Clear");
    exportDirCleanButton.setVisible(false);
    exportDirButtons.add(exportDirCleanButton);
    exportDirChooseButton = new JButton();
    exportDirChooseButton.setText("Choose...");
    exportDirChooseButton.setVisible(true);
    exportDirButtons.add(exportDirChooseButton);
    pemFileButtons = new JPanel();
    pemFileButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(pemFileButtons, gbc);
    pemFileClearButton = new JButton();
    pemFileClearButton.setText("Clear");
    pemFileClearButton.setVisible(false);
    pemFileButtons.add(pemFileClearButton);
    pemFileChooseButton = new JButton();
    pemFileChooseButton.setText("Choose...");
    pemFileChooseButton.setVisible(true);
    pemFileButtons.add(pemFileChooseButton);
    overwriteFilesField = new JCheckBox();
    overwriteFilesField.setText("Overwrite existing files");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 9;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(overwriteFilesField, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 8;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
