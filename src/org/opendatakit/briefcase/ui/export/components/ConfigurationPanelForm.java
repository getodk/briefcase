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

import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;
import static org.opendatakit.briefcase.ui.reused.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.opendatakit.briefcase.ui.MessageStrings;
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
  private final List<Consumer<Path>> onSelectExportDirCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onSelectPemFileCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectStartDateCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectEndDateCallbacks = new ArrayList<>();
  private boolean clearableExportDir;

  ConfigurationPanelForm(boolean clearableExportDir) {
    this.clearableExportDir = clearableExportDir;
    startDatePicker = createDatePicker();
    endDatePicker = createDatePicker();
    $$$setupUI$$$();
    startDatePicker.getSettings().setGapBeforeButtonPixels(0);
    startDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    startDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());
    endDatePicker.getSettings().setGapBeforeButtonPixels(0);
    endDatePicker.getComponentDateTextField().setPreferredSize(exportDirField.getPreferredSize());
    endDatePicker.getComponentToggleCalendarButton().setPreferredSize(exportDirChooseButton.getPreferredSize());

    exportDirChooseButton.addActionListener(__ ->
        buildExportDirDialog().choose().ifPresent(file -> setExportDir(Paths.get(file.toURI())))
    );
    exportDirCleanButton.addActionListener(__ -> clearExportDir());
    pemFileChooseButton.addActionListener(__ ->
        buildPemFileDialog().choose().ifPresent(file -> setPemFile(Paths.get(file.toURI())))
    );
    pemFileClearButton.addActionListener(__ -> clearPemFile());
    startDatePicker.addDateChangeListener(event -> {
      if (!isDateRangeValid()) {
        showError(MessageStrings.INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        startDatePicker.clear();
      } else
        onSelectStartDateCallbacks.forEach(consumer -> consumer.accept(extractDate(event)));
    });
    endDatePicker.addDateChangeListener(event -> {
      if (!isDateRangeValid()) {
        showError(MessageStrings.INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        endDatePicker.clear();
      } else
        onSelectEndDateCallbacks.forEach(consumer -> consumer.accept(extractDate(event)));
    });
  }

  private boolean isDateRangeValid() {
    LocalDate startDate = startDatePicker.getDate();
    LocalDate endDate = endDatePicker.getDate();
    return startDate == null || endDate == null || startDate.isBefore(endDate);
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
    if (clearableExportDir) {
      exportDirChooseButton.setVisible(false);
      exportDirCleanButton.setVisible(true);
    }
  }

  void clearExportDir() {
    exportDirField.setText(null);
    onSelectExportDirCallbacks.forEach(consumer -> consumer.accept(null));
    if (clearableExportDir) {
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

  protected void showError(String message, String title) {
    showErrorDialog(container, message, title);
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

  private static LocalDate extractDate(DateChangeEvent event) {
    return event.getNewDate() != null ? LocalDate.of(
        event.getNewDate().getYear(),
        event.getNewDate().getMonthValue(),
        event.getNewDate().getDayOfMonth()
    ) : null;
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
    pemFileButtons = new JPanel();
    pemFileButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(pemFileButtons, gbc);
    pemFileChooseButton = new JButton();
    pemFileChooseButton.setText("Choose...");
    pemFileChooseButton.setVisible(true);
    pemFileButtons.add(pemFileChooseButton);
    pemFileClearButton = new JButton();
    pemFileClearButton.setText("Clear");
    pemFileClearButton.setVisible(false);
    pemFileButtons.add(pemFileClearButton);
    exportDirButtons = new JPanel();
    exportDirButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(exportDirButtons, gbc);
    exportDirChooseButton = new JButton();
    exportDirChooseButton.setText("Choose...");
    exportDirChooseButton.setVisible(true);
    exportDirButtons.add(exportDirChooseButton);
    exportDirCleanButton = new JButton();
    exportDirCleanButton.setText("Clear");
    exportDirCleanButton.setVisible(false);
    exportDirButtons.add(exportDirCleanButton);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
