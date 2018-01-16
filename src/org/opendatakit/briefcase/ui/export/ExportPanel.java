/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui.export;

import static java.lang.Short.MAX_VALUE;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_DIRECTORY;
import static org.opendatakit.briefcase.ui.MessageStrings.DIR_NOT_EXIST;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.export.FileChooser.directory;
import static org.opendatakit.briefcase.ui.export.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;
import static org.threeten.bp.format.DateTimeFormatter.ISO_DATE;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.StringUtils;
import org.threeten.bp.LocalDate;

public class ExportPanel extends JPanel {

  private static final long serialVersionUID = 7169316129011796197L;
  private static final BriefcasePreferences PREFERENCES = BriefcasePreferences.forClass(ExportPanel.class);
  private static final String EXPORT_DIR_PREF = "exportDir";
  private static final String PEM_FILE_PREF = "pemFile";
  private static final String EXPORT_START_DATE_PREF = "exportStartDate";
  private static final String EXPORT_END_DATE_PREF = "exportEndDate";

  public static final String TAB_NAME = "Export";

  final JTextField txtExportDirectory;
  private final JButton btnChooseExportDirectory;

  private final JTextField pemPrivateKeyFilePath;
  private final JButton btnPemFileChooseButton;

  private final DatePicker pickStartDate;
  private final DatePicker pickEndDate;

  private final FormExportTableModel tableModel;

  private final JButton btnSelectAll;
  private final JButton btnClearAll;

  private final JButton btnExport;

  private boolean exportStateActive = false;

  private final TerminationFuture terminationFuture;

  public ExportPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    JLabel lblExportDirectory = new JLabel("Export Directory:");

    txtExportDirectory = new JTextField();
    txtExportDirectory.setFocusable(false);
    txtExportDirectory.setEditable(false);
    txtExportDirectory.setColumns(10);
    txtExportDirectory.setText(PREFERENCES.get(EXPORT_DIR_PREF, ""));

    btnChooseExportDirectory = new JButton("Choose...");
    btnChooseExportDirectory.addActionListener(__ -> getExportDirectoryChooser().choose()
        .ifPresent(file -> {
          txtExportDirectory.setText(file.getAbsolutePath());
          updateExportButton();
          PREFERENCES.put(EXPORT_DIR_PREF, file.getAbsolutePath());
        }));

    JLabel lblPemPrivateKey = new JLabel("PEM Private Key File:");

    pemPrivateKeyFilePath = new JTextField();
    pemPrivateKeyFilePath.setFocusable(false);
    pemPrivateKeyFilePath.setEditable(false);
    pemPrivateKeyFilePath.setColumns(10);
    pemPrivateKeyFilePath.setText(PREFERENCES.get(PEM_FILE_PREF, ""));

    btnPemFileChooseButton = new JButton("Choose...");
    btnPemFileChooseButton.addActionListener(__ -> getPemFileChooser().choose().ifPresent(file -> {
      pemPrivateKeyFilePath.setText(file.getAbsolutePath());
      updateExportButton();
      PREFERENCES.put(PEM_FILE_PREF, file.getAbsolutePath());
    }));

    JLabel lblDateFrom = new JLabel("Start Date (inclusive):");
    JLabel lblDateTo = new JLabel("End Date (exclusive):");

    pickStartDate = createDatePicker();
    pickStartDate.setDate(PREFERENCES.get(EXPORT_START_DATE_PREF, LocalDate::parse, null));
    pickStartDate.addDateChangeListener(__ -> updateExportButton());
    pickStartDate.addDateChangeListener(__ -> validateDate(pickStartDate));
    pickStartDate.addDateChangeListener(event -> PREFERENCES.put(
        EXPORT_START_DATE_PREF,
        Optional.ofNullable(event.getNewDate()).map(ld -> ld.format(ISO_DATE)).orElse(null)
    ));
    pickEndDate = createDatePicker();
    pickEndDate.setDate(PREFERENCES.get(EXPORT_END_DATE_PREF, LocalDate::parse, null));
    pickEndDate.addDateChangeListener(__ -> updateExportButton());
    pickEndDate.addDateChangeListener(__ -> validateDate(pickEndDate));
    pickEndDate.addDateChangeListener(event -> PREFERENCES.put(
        EXPORT_END_DATE_PREF,
        Optional.ofNullable(event.getNewDate()).map(ld -> ld.format(ISO_DATE)).orElse(null))
    );

    tableModel = new FormExportTableModel();
    tableModel.onSelectionChange(this::updateExportButton);
    tableModel.onSelectionChange(this::updateSelectAllButton);
    tableModel.onSelectionChange(this::updateClearAllButton);

    btnSelectAll = new JButton("Select all");
    btnSelectAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> tableModel.checkAll()).build());

    btnClearAll = new JButton("Clear all");
    btnClearAll.setVisible(false);
    btnClearAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> tableModel.uncheckAll()).build());

    JLabel lblFormsToTransfer = new JLabel("Forms to export:");

    FormExportTable formTable = new FormExportTable(tableModel);
    formTable.setName("forms");
    JScrollPane scrollPane = new JScrollPane(formTable);
    JSeparator separatorFormsList = new JSeparator();

    btnExport = new JButton("Export");
    btnExport.setName("export");
    btnExport.setEnabled(false);
    btnExport.addActionListener(__ -> {
      List<String> configurationErrors = getErrors();
      showErrors(
          configurationErrors,
          "We can't export the forms until these errors are corrected:",
          "Export configuration error"
      );
      if (configurationErrors.isEmpty()) {
        new Thread(() -> {
          btnExport.setEnabled(false);
          List<String> errors = export();
          showErrors(
              errors,
              "We have found some errors while performing the requested export actions:",
              "Export error report"
          );
          btnExport.setEnabled(true);
        }).start();
      }
    });

    GroupLayout groupLayout = new GroupLayout(this);

    GroupLayout.ParallelGroup labels = groupLayout.createParallelGroup(LEADING)
        .addComponent(lblExportDirectory)
        .addComponent(lblPemPrivateKey)
        .addComponent(lblDateFrom)
        .addComponent(lblDateTo);

    GroupLayout.ParallelGroup fields = groupLayout.createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addComponent(txtExportDirectory)
            .addPreferredGap(RELATED)
            .addComponent(btnChooseExportDirectory))
        .addGroup(groupLayout.createSequentialGroup()
            .addComponent(pemPrivateKeyFilePath)
            .addPreferredGap(RELATED)
            .addComponent(btnPemFileChooseButton))
        .addComponent(pickStartDate)
        .addComponent(pickEndDate);

    GroupLayout.SequentialGroup leftActions = groupLayout.createSequentialGroup()
        .addComponent(btnSelectAll)
        .addComponent(btnClearAll);

    GroupLayout.SequentialGroup rightActions = groupLayout.createSequentialGroup()
        .addComponent(btnExport);

    GroupLayout.SequentialGroup horizontalGroup = groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(LEADING)
            .addGroup(groupLayout.createSequentialGroup()
                .addGroup(labels)
                .addPreferredGap(RELATED)
                .addGroup(fields)
            )
            .addComponent(separatorFormsList, DEFAULT_SIZE, PREFERRED_SIZE, MAX_VALUE)
            .addComponent(lblFormsToTransfer)
            .addComponent(scrollPane, PREFERRED_SIZE, PREFERRED_SIZE, MAX_VALUE)
            .addGroup(LEADING, leftActions)
            .addGroup(TRAILING, rightActions)
        )
        .addContainerGap();


    GroupLayout.ParallelGroup verticalGroup = groupLayout
        .createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(lblExportDirectory)
                .addComponent(btnChooseExportDirectory)
                .addComponent(txtExportDirectory, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            )
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(lblPemPrivateKey)
                .addComponent(pemPrivateKeyFilePath, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(btnPemFileChooseButton)).addPreferredGap(RELATED)
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(lblDateFrom)
                .addComponent(pickStartDate))
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(lblDateTo)
                .addComponent(pickEndDate))
            .addPreferredGap(ComponentPlacement.UNRELATED, 10, MAX_VALUE)
            .addComponent(separatorFormsList, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(lblFormsToTransfer)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(scrollPane, 200, PREFERRED_SIZE, MAX_VALUE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(groupLayout.createParallelGroup(TRAILING)
                .addComponent(btnSelectAll)
                .addComponent(btnClearAll)
                .addComponent(btnExport))
            .addContainerGap());

    groupLayout.setHorizontalGroup(horizontalGroup);
    groupLayout.setVerticalGroup(verticalGroup);
    setLayout(groupLayout);
    updateForms();
    setActiveExportState(exportStateActive);
  }

  private FileChooser getExportDirectoryChooser() {
    return directory(
        this,
        fileFrom(txtExportDirectory),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    );
  }

  private FileChooser getPemFileChooser() {
    return file(
        this,
        fileFrom(pemPrivateKeyFilePath)
    );
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    String exportDirText = txtExportDirectory.getText().trim();
    Path exportDir = Paths.get(exportDirText);

    if (StringUtils.nullOrEmpty(exportDirText))
      errors.add("Export directory was not specified.");

    if (StringUtils.notEmpty(exportDirText) && !exists(exportDir))
      errors.add(DIR_NOT_EXIST);

    if (StringUtils.notEmpty(exportDirText) && exists(exportDir) && !isDirectory(exportDir))
      errors.add(DIR_NOT_DIRECTORY);

    if (exists(exportDir) && isDirectory(exportDir) && isUnderODKFolder(exportDir.toFile()))
      errors.add(DIR_INSIDE_ODK_DEVICE_DIRECTORY);

    if (exists(exportDir) && isDirectory(exportDir) && isUnderBriefcaseFolder(exportDir.toFile()))
      errors.add(DIR_INSIDE_BRIEFCASE_STORAGE);

    Date fromDate = pickStartDate.convert().getDateWithDefaultZone();
    Date toDate = pickEndDate.convert().getDateWithDefaultZone();
    if (fromDate != null && toDate == null)
      errors.add("Missing date range end definition");
    if (fromDate == null && toDate != null)
      errors.add("Missing date range start definition");
    if (fromDate != null && toDate != null && fromDate.compareTo(toDate) >= 0)
      errors.add(INVALID_DATE_RANGE_MESSAGE);

    if (tableModel.noneSelected())
      errors.add("No form has been selected");

    return errors;
  }

  private void updateExportButton() {
    btnExport.setEnabled(getErrors().isEmpty());
  }

  private void updateClearAllButton() {
    btnClearAll.setVisible(tableModel.allSelected());
  }

  private void updateSelectAllButton() {
    btnSelectAll.setVisible(!tableModel.allSelected());
  }

  public void updateForms() {
    tableModel.setForms(FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList()));
  }

  private Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::nullOrEmpty)
        .map(path -> Paths.get(path).toFile());
  }

  private void validateDate(DatePicker targetDatePicker) {
    Date fromDate = pickStartDate.convert().getDateWithDefaultZone();
    Date toDate = pickEndDate.convert().getDateWithDefaultZone();
    if (fromDate != null && toDate != null && fromDate.compareTo(toDate) >= 0) {
      showErrorDialog(this, INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
      targetDatePicker.clear();
    }
  }

  private void showErrors(List<String> errors, String headerText, String title) {
    if (!errors.isEmpty()) {
      String message = String.format(
          "%s\n\n%s",
          headerText,
          errors.stream().map(e -> "- " + e).collect(joining("\n"))
      );
      showErrorDialog(this, message, title);
    }
  }

  /**
   * The DatePicker default text box and calendar button don't match with the rest of the UI.
   * This tweaks those elements to be consistent with the rest of the application.
   */
  private DatePicker createDatePicker() {
    DatePicker datePicker = new DatePicker();
    JTextField model = new JTextField();

    datePicker.getComponentToggleCalendarButton().setText("Choose...");
    datePicker.getComponentDateTextField().setBorder(model.getBorder());
    datePicker.getComponentDateTextField().setMargin(model.getMargin());

    return datePicker;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateForms();

    for (Component c : this.getComponents()) {
      c.setEnabled(enabled);
    }
    if (enabled) {
      // and then update the widgets based upon the transfer state
      setActiveExportState(exportStateActive);
    }
  }

  private void setActiveExportState(boolean active) {
    if (active) {
      // don't allow normal actions when we are transferring...
      btnChooseExportDirectory.setEnabled(false);
      btnPemFileChooseButton.setEnabled(false);
      pickStartDate.setEnabled(false);
      pickEndDate.setEnabled(false);
      btnExport.setEnabled(false);
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
    } else {
      btnChooseExportDirectory.setEnabled(true);
      btnPemFileChooseButton.setEnabled(true);
      pickStartDate.setEnabled(true);
      pickEndDate.setEnabled(true);
      btnExport.setEnabled(true);
      // touch-up with real state...
      updateExportButton();
      // retain progress text (to display last export outcome)
    }
    // remember state...
    exportStateActive = active;
  }

  void resetExport() {
  }

  private List<String> export() {
    return tableModel.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(formDefinition).stream())
        .collect(toList());
  }

  private List<String> export(BriefcaseFormDefinition formDefinition) {
    List<String> errors = new ArrayList<>();
    Optional<File> pemFile = Optional.ofNullable(pemPrivateKeyFilePath.getText())
        .map(File::new)
        .filter(File::exists);
    if ((formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) && !pemFile.isPresent())
      errors.add(formDefinition.getFormName() + " form requires is encrypted and you haven't defined a valid private key file location");
    else
      try {
        ExportAction.export(
            new File(txtExportDirectory.getText().trim()),
            ExportType.CSV,
            formDefinition,
            pemFile.orElse(null),
            terminationFuture,
            pickStartDate.convert().getDateWithDefaultZone(),
            pickEndDate.convert().getDateWithDefaultZone()
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void onTransferSucceededEvent(TransferSucceededEvent event) {
    updateForms();
  }
}
