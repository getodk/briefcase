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

import static org.opendatakit.briefcase.reused.TriStateBoolean.TRUE;
import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;
import static org.opendatakit.briefcase.ui.reused.FileChooser.file;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.UI.confirm;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;
import static org.opendatakit.briefcase.util.Host.isMac;
import static org.opendatakit.briefcase.util.Host.isWindows;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.Color;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import org.opendatakit.briefcase.export.DateRange;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.reused.TriStateBoolean;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.StringUtils;

@SuppressWarnings("checkstyle:MethodName")
public class ConfigurationPanelForm extends JComponent {
  private JPanel container;

  // UI components
  final DatePicker startDatePicker;
  final DatePicker endDatePicker;
  JTextField exportDirField;
  JTextField pemFileField;
  JButton exportDirChooseButton;
  private JLabel exportDirLabel;
  private JLabel pemFileLabel;
  private JLabel startDateLabel;
  private JLabel endDateLabel;
  private JPanel pemFileButtons;
  JButton pemFileChooseButton;
  JButton pemFileClearButton;
  private JPanel exportDirButtons;
  private JButton exportDirCleanButton;
  JCheckBox pullBeforeField;
  JLabel pullBeforeOverrideLabel;
  JTextPane pullBeforeHintPanel;
  final CustomConfBooleanForm pullBeforeOverrideField;
  JCheckBox overwriteFilesField;
  JLabel overwriteFilesOverrideLabel;
  final CustomConfBooleanForm overwriteFilesOverrideField;
  JCheckBox exportMediaField;
  JLabel exportMediaOverrideLabel;
  final CustomConfBooleanForm exportMediaOverrideField;
  JCheckBox splitSelectMultiplesField;
  JLabel splitSelectMultiplesOverrideLabel;
  final CustomConfBooleanForm splitSelectMultiplesOverrideField;
  JCheckBox includeGeoJsonExportField;
  JLabel includeGeoJsonExportOverrideLabel;
  final CustomConfBooleanForm includeGeoJsonExportOverrideField;
  JCheckBox removeGroupNamesField;
  JLabel removeGroupNamesOverrideLabel;
  final CustomConfBooleanForm removeGroupNamesOverrideField;

  // UI status and callbacks
  private final ConfigurationPanelMode mode;
  private boolean uiLocked = false;
  private final List<Consumer<ExportConfiguration>> onChangeCallbacks = new ArrayList<>();

  // UI state
  private Optional<Path> exportDir = Optional.empty();
  private Optional<Path> pemFile = Optional.empty();
  private DateRange dateRange = DateRange.empty();
  private OverridableBoolean pullBefore = OverridableBoolean.FALSE;
  private OverridableBoolean overwriteFiles = OverridableBoolean.FALSE;
  private OverridableBoolean exportMedia = OverridableBoolean.TRUE;
  private OverridableBoolean splitSelectMultiples = OverridableBoolean.FALSE;
  private OverridableBoolean includeGeoJsonExport = OverridableBoolean.FALSE;
  private OverridableBoolean removeGroupNames = OverridableBoolean.FALSE;

  ConfigurationPanelForm(ConfigurationPanelMode mode) {
    this.mode = mode;

    // Set the UI up
    startDatePicker = createDatePicker();
    endDatePicker = createDatePicker();
    pullBeforeOverrideField = new CustomConfBooleanForm(Optional.empty());
    exportMediaOverrideField = new CustomConfBooleanForm(Optional.empty());
    overwriteFilesOverrideField = new CustomConfBooleanForm(Optional.empty());
    splitSelectMultiplesOverrideField = new CustomConfBooleanForm(Optional.empty());
    includeGeoJsonExportOverrideField = new CustomConfBooleanForm(Optional.empty());
    removeGroupNamesOverrideField = new CustomConfBooleanForm(Optional.empty());

    $$$setupUI$$$();

    startDatePicker.getSettings().setGapBeforeButtonPixels(0);
    startDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    startDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());

    endDatePicker.getSettings().setGapBeforeButtonPixels(0);
    endDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    endDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());

    pullBeforeHintPanel.setBackground(new Color(255, 255, 255, 0));

    // Deal with platform quirks
    GridBagLayout layout = (GridBagLayout) container.getLayout();
    GridBagConstraints constraints = layout.getConstraints(pullBeforeHintPanel);
    constraints.insets = new Insets(0, isMac() ? 6 : isWindows() ? 2 : 0, 0, 0);
    layout.setConstraints(pullBeforeHintPanel, constraints);

    // Unlock the UI
    mode.decorate(this, false);
    attachUI();
  }

  private void attachUI() {
    // Attach UI component listeners
    exportDirChooseButton.addActionListener(__ -> buildExportDirDialog().choose().ifPresent(file -> setExportDir(Paths.get(file.toURI()))));
    exportDirCleanButton.addActionListener(__ -> clearExportDir());
    pemFileChooseButton.addActionListener(__ -> buildPemFileDialog().choose().ifPresent(file -> setPemFile(Paths.get(file.toURI()))));
    pemFileClearButton.addActionListener(__ -> clearPemFile());
    startDatePicker.addDateChangeListener(event -> setStartDate(event.getNewDate()));
    endDatePicker.addDateChangeListener(event -> setEndDate(event.getNewDate()));
    pullBeforeField.addActionListener(__ -> setPullBefore(pullBeforeField.isSelected()));
    pullBeforeOverrideField.onChange(this::setPullBefore);
    overwriteFilesField.addActionListener(__ -> setOverwriteFiles(overwriteFilesField.isSelected()));
    overwriteFilesOverrideField.onChange(this::setOverwriteFiles);
    exportMediaField.addActionListener(__ -> setExportMedia(exportMediaField.isSelected()));
    exportMediaOverrideField.onChange(this::setExportMedia);
    splitSelectMultiplesField.addActionListener(__ -> setSplitSelectMultiples(splitSelectMultiplesField.isSelected()));
    splitSelectMultiplesOverrideField.onChange(this::setSplitSelectMultiples);
    includeGeoJsonExportField.addActionListener(__ -> setIncludeGeoJsonExport(includeGeoJsonExportField.isSelected()));
    includeGeoJsonExportOverrideField.onChange(this::setIncludeGeoJsonExport);
    removeGroupNamesField.addActionListener(__ -> setRemoveGroupNames(removeGroupNamesField.isSelected()));
    removeGroupNamesOverrideField.onChange(this::setRemoveGroupNames);
  }

  void initialize(ExportConfiguration configuration) {
    configuration.ifExportDirPresent(this::setExportDir);
    configuration.ifPemFilePresent(this::setPemFile);
    setDateRange(configuration.getDateRange());
    setPullBefore(configuration.getPullBefore());
    setOverwriteFiles(configuration.getOverwriteFiles());
    setExportMedia(configuration.getExportMedia());
    setSplitSelectMultiples(configuration.getSplitSelectMultiples());
    setIncludeGeoJsonExport(configuration.getIncludeGeoJsonExport());
    setRemoveGroupNames(configuration.getRemoveGroupNames());
  }

  @Override
  public void setEnabled(boolean enabled) {
    uiLocked = !enabled;
    container.setEnabled(enabled);
    exportDirField.setEnabled(enabled);
    exportDirLabel.setEnabled(enabled);
    exportDirButtons.setEnabled(enabled);
    exportDirChooseButton.setEnabled(enabled);
    exportDirCleanButton.setEnabled(enabled);
    pemFileField.setEnabled(enabled);
    pemFileLabel.setEnabled(enabled);
    pemFileButtons.setEnabled(enabled);
    pemFileChooseButton.setEnabled(enabled);
    pemFileClearButton.setEnabled(enabled);
    startDatePicker.setEnabled(enabled);
    startDateLabel.setEnabled(enabled);
    endDatePicker.setEnabled(enabled);
    endDateLabel.setEnabled(enabled);
    pullBeforeField.setEnabled(enabled);
    pullBeforeOverrideField.setEnabled(enabled);
    pullBeforeHintPanel.setEnabled(enabled);
    pullBeforeOverrideLabel.setEnabled(enabled);
    overwriteFilesField.setEnabled(enabled);
    overwriteFilesOverrideField.setEnabled(enabled);
    overwriteFilesOverrideLabel.setEnabled(enabled);
    exportMediaField.setEnabled(enabled);
    exportMediaOverrideField.setEnabled(enabled);
    exportMediaOverrideLabel.setEnabled(enabled);
    splitSelectMultiplesField.setEnabled(enabled);
    splitSelectMultiplesOverrideField.setEnabled(enabled);
    splitSelectMultiplesOverrideLabel.setEnabled(enabled);
    includeGeoJsonExportField.setEnabled(enabled);
    includeGeoJsonExportOverrideField.setEnabled(enabled);
    includeGeoJsonExportOverrideLabel.setEnabled(enabled);
    removeGroupNamesField.setEnabled(enabled);
    removeGroupNamesOverrideField.setEnabled(enabled);
    removeGroupNamesOverrideLabel.setEnabled(enabled);
  }

  void setExportDir(Path path) {
    exportDir = Optional.of(path);
    exportDirField.setText(path.toString());
    if (mode.isExportDirCleanable()) {
      exportDirChooseButton.setVisible(false);
      exportDirCleanButton.setVisible(true);
    }
    triggerOnChange();
  }

  void clearExportDir() {
    exportDir = Optional.empty();
    exportDirField.setText(null);
    if (mode.isExportDirCleanable()) {
      exportDirChooseButton.setVisible(true);
      exportDirCleanButton.setVisible(false);
    }
    triggerOnChange();
  }

  void setPemFile(Path path) {
    pemFile = Optional.of(path);
    pemFileField.setText(path.toString());
    pemFileChooseButton.setVisible(false);
    pemFileClearButton.setVisible(true);
    triggerOnChange();
  }

  private void clearPemFile() {
    pemFile = Optional.empty();
    pemFileField.setText(null);
    pemFileChooseButton.setVisible(true);
    pemFileClearButton.setVisible(false);
    triggerOnChange();
  }

  private void setDateRange(DateRange range) {
    dateRange = range;
    dateRange.ifStartPresent(date -> {
      startDatePicker.setDate(date);
      endDatePicker.getSettings().setDateRangeLimits(date, null);
      triggerOnChange();
    });
    dateRange.ifEndPresent(date -> {
      endDatePicker.setDate(date);
      startDatePicker.getSettings().setDateRangeLimits(null, date);
      triggerOnChange();
    });
  }

  void setStartDate(LocalDate date) {
    // Prevent dupe calls to this method from the picker's listener
    if (date == null || !dateRange.isStartPresent() || !dateRange.getStart().equals(date)) {
      dateRange = dateRange.setStart(date);
      startDatePicker.setDate(date);
      endDatePicker.getSettings().setDateRangeLimits(date, null);
      triggerOnChange();
    }
  }

  void setEndDate(LocalDate date) {
    // Prevent dupe calls to this method from the picker's listener
    if (date == null || !dateRange.isEndPresent() || !dateRange.getEnd().equals(date)) {
      dateRange = dateRange.setEnd(date);
      endDatePicker.setDate(date);
      startDatePicker.getSettings().setDateRangeLimits(null, date);
      triggerOnChange();
    }
  }

  void setPullBefore(boolean enabled) {
    pullBefore = pullBefore.set(enabled);
    triggerOnChange();
  }

  void setPullBefore(TriStateBoolean overrideValue) {
    pullBefore = pullBefore.overrideWith(overrideValue);
    triggerOnChange();
  }

  private void setPullBefore(OverridableBoolean value) {
    pullBefore = value;
    pullBeforeField.setSelected(value.get(false));
    pullBeforeOverrideField.set(value.getOverride());
  }

  private void setOverwriteFiles(boolean enabled) {
    if (!enabled || confirmOverwriteFiles()) {
      overwriteFiles = overwriteFiles.set(enabled);
      triggerOnChange();
    } else
      overwriteFilesField.setSelected(false);
  }

  private void setOverwriteFiles(TriStateBoolean overrideValue) {
    TriStateBoolean previousValue = overwriteFiles.getOverride();
    if (previousValue != overrideValue && (overrideValue != TRUE || confirmOverwriteFiles())) {
      overwriteFiles = overwriteFiles.overrideWith(overrideValue);
      triggerOnChange();
    } else
      overwriteFilesOverrideField.set(previousValue);
  }

  private void setOverwriteFiles(OverridableBoolean value) {
    overwriteFiles = value;
    overwriteFilesField.setSelected(value.get(false));
    overwriteFilesOverrideField.set(value.getOverride());
  }

  private void setExportMedia(boolean enabled) {
    exportMedia = exportMedia.set(enabled);
    triggerOnChange();
  }

  private void setExportMedia(TriStateBoolean overrideValue) {
    exportMedia = exportMedia.overrideWith(overrideValue);
    triggerOnChange();
  }

  private void setExportMedia(OverridableBoolean value) {
    exportMedia = value;
    exportMediaField.setSelected(value.get(true));
    exportMediaOverrideField.set(value.getOverride());
  }

  private void setSplitSelectMultiples(boolean enabled) {
    splitSelectMultiples = splitSelectMultiples.set(enabled);
    triggerOnChange();
  }

  private void setSplitSelectMultiples(TriStateBoolean overrideValue) {
    splitSelectMultiples = splitSelectMultiples.overrideWith(overrideValue);
    triggerOnChange();
  }

  private void setSplitSelectMultiples(OverridableBoolean value) {
    splitSelectMultiples = value;
    splitSelectMultiplesField.setSelected(value.get(false));
    splitSelectMultiplesOverrideField.set(value.getOverride());
  }

  private void setIncludeGeoJsonExport(boolean enabled) {
    includeGeoJsonExport = includeGeoJsonExport.set(enabled);
    triggerOnChange();
  }

  private void setIncludeGeoJsonExport(TriStateBoolean overrideValue) {
    includeGeoJsonExport = includeGeoJsonExport.overrideWith(overrideValue);
    triggerOnChange();
  }

  private void setIncludeGeoJsonExport(OverridableBoolean value) {
    includeGeoJsonExport = value;
    includeGeoJsonExportField.setSelected(value.get(false));
    includeGeoJsonExportOverrideField.set(value.getOverride());
  }

  private void setRemoveGroupNames(boolean enabled) {
    removeGroupNames = removeGroupNames.set(enabled);
    triggerOnChange();
  }

  private void setRemoveGroupNames(TriStateBoolean overrideValue) {
    removeGroupNames = removeGroupNames.overrideWith(overrideValue);
    triggerOnChange();
  }

  private void setRemoveGroupNames(OverridableBoolean value) {
    removeGroupNames = value;
    removeGroupNamesField.setSelected(value.get(false));
    removeGroupNamesOverrideField.set(value.getOverride());
  }

  void onChange(Consumer<ExportConfiguration> callback) {
    onChangeCallbacks.add(callback);
  }

  private void triggerOnChange() {
    ExportConfiguration conf = ExportConfiguration.Builder.empty()
        .setExportDir(exportDir, error -> {
          errorMessage("Invalid export dir", error);
          clearExportDir();
        })
        .setPemFile(pemFile, error -> {
          errorMessage("Invalid PEM file selected", error);
          clearPemFile();
        })
        .setDateRange(dateRange)
        .setPullBefore(pullBefore)
        .setOverwriteFiles(overwriteFiles)
        .setExportMedia(exportMedia)
        .setSplitSelectMultiples(splitSelectMultiples)
        .setIncludeGeoJsonExport(includeGeoJsonExport)
        .setRemoveGroupNames(removeGroupNames)
        .build();
    onChangeCallbacks.forEach(callback -> callback.accept(conf));
  }

  public ExportConfiguration getConfiguration() {
    return null;
  }

  void changeMode(boolean savePasswordsConsent) {
    mode.setSavePasswordsConsent(savePasswordsConsent);
    mode.decorate(this, uiLocked);
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

  private boolean confirmOverwriteFiles() {
    if (confirm("The default behavior is to append to existing files. Are you sure you want to overwrite existing files?"))
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
    startDateLabel.setText("Start date (inclusive)");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(startDateLabel, gbc);
    endDateLabel = new JLabel();
    endDateLabel.setText("End date (inclusive)");
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
    gbc.gridy = 10;
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
    gbc.gridy = 17;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(pullBeforeHintPanel, gbc);
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
    gbc.gridy = 16;
    gbc.anchor = GridBagConstraints.EAST;
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
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 18;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
    exportMediaField = new JCheckBox();
    exportMediaField.setText("Export media files");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(exportMediaField, gbc);
    exportMediaOverrideLabel = new JLabel();
    exportMediaOverrideLabel.setText("Export media files");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 11;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(exportMediaOverrideLabel, gbc);
    overwriteFilesField = new JCheckBox();
    overwriteFilesField.setText("Overwrite existing files");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(overwriteFilesField, gbc);
    overwriteFilesOverrideLabel = new JLabel();
    overwriteFilesOverrideLabel.setText("Overwrite existing files");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 12;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(overwriteFilesOverrideLabel, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 12;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(overwriteFilesOverrideField.$$$getRootComponent$$$(), gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 11;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(exportMediaOverrideField.$$$getRootComponent$$$(), gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 16;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(pullBeforeOverrideField.$$$getRootComponent$$$(), gbc);
    splitSelectMultiplesOverrideLabel = new JLabel();
    splitSelectMultiplesOverrideLabel.setText("Split select multiples");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 13;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(splitSelectMultiplesOverrideLabel, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 13;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(splitSelectMultiplesOverrideField.$$$getRootComponent$$$(), gbc);
    splitSelectMultiplesField = new JCheckBox();
    splitSelectMultiplesField.setText("Split select multiples");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 7;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(splitSelectMultiplesField, gbc);
    includeGeoJsonExportField = new JCheckBox();
    includeGeoJsonExportField.setText("Include GeoJSON export");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 8;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(includeGeoJsonExportField, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 14;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(includeGeoJsonExportOverrideField.$$$getRootComponent$$$(), gbc);
    includeGeoJsonExportOverrideLabel = new JLabel();
    includeGeoJsonExportOverrideLabel.setText("Include GeoJSON export");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 14;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(includeGeoJsonExportOverrideLabel, gbc);
    removeGroupNamesField = new JCheckBox();
    removeGroupNamesField.setText("Remove group names");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 9;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(removeGroupNamesField, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 15;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(removeGroupNamesOverrideField.$$$getRootComponent$$$(), gbc);
    removeGroupNamesOverrideLabel = new JLabel();
    removeGroupNamesOverrideLabel.setText("Remove group names");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 15;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(removeGroupNamesOverrideLabel, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }

}
