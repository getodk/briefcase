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
import static java.util.stream.Collectors.toList;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.export.FileChooser.directory;
import static org.opendatakit.briefcase.ui.export.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.Component;
import java.io.File;
import java.nio.file.Paths;
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
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportProgressPercentageEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.model.UpdatedBriefcaseFormDefinitionEvent;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.StringUtils;

public class ExportPanel extends JPanel {

  private static final long serialVersionUID = 7169316129011796197L;

  public static final String TAB_NAME = "Export";

  final JTextField txtExportDirectory;
  private final JButton btnChooseExportDirectory;

  final JTextField pemPrivateKeyFilePath;
  private final JButton btnPemFileChooseButton;

  final DatePicker pickStartDate;
  final DatePicker pickEndDate;

  private final FormExportTableModel tableModel;

  private final JButton btnSelectAll;
  private final JButton btnClearAll;

  private final JButton btnExport;

  private boolean exportStateActive = false;

  final TerminationFuture terminationFuture;

  void enableExportButton() {
    String exportDir = txtExportDirectory.getText();
    if (exportDir == null || exportDir.trim().length() == 0) {
      btnExport.setEnabled(false);
      return;
    }

    boolean enabled = true;
    File exportDirectory = new File(exportDir.trim());
    if (!exportDirectory.exists()) {
      enabled = false;
    }
    if (!exportDirectory.isDirectory()) {
      enabled = false;
    }
    if (FileSystemUtils.isUnderODKFolder(exportDirectory)) {
      enabled = false;
    } else if (isUnderBriefcaseFolder(exportDirectory)) {
      enabled = false;
    }
    btnExport.setEnabled(enabled);
  }

  public ExportPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    JLabel lblExportDirectory = new JLabel("Export Directory:");

    txtExportDirectory = new JTextField();
    txtExportDirectory.setFocusable(false);
    txtExportDirectory.setEditable(false);
    txtExportDirectory.setColumns(10);

    btnChooseExportDirectory = new JButton("Choose...");
    btnChooseExportDirectory.addActionListener(__ -> chooseLocation(directory(
        this,
        fileFrom(txtExportDirectory),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    ), txtExportDirectory));

    JLabel lblPemPrivateKey = new JLabel("PEM Private Key File:");

    pemPrivateKeyFilePath = new JTextField();
    pemPrivateKeyFilePath.setFocusable(false);
    pemPrivateKeyFilePath.setEditable(false);
    pemPrivateKeyFilePath.setColumns(10);

    btnPemFileChooseButton = new JButton("Choose...");
    btnPemFileChooseButton.addActionListener(__ -> chooseLocation(file(
        this,
        fileFrom(pemPrivateKeyFilePath)
    ), pemPrivateKeyFilePath));

    JLabel lblDateFrom = new JLabel("Start Date (inclusive):");
    JLabel lblDateTo = new JLabel("End Date (exclusive):");

    pickStartDate = createDatePicker();
    pickEndDate = createDatePicker();

    tableModel = new FormExportTableModel();

    btnSelectAll = new JButton("Select all");

    btnClearAll = new JButton("Clear all");

    JLabel lblFormsToTransfer = new JLabel("Forms to export:");

    JScrollPane scrollPane = new JScrollPane(new FormExportTable(tableModel));
    JSeparator separatorFormsList = new JSeparator();

    btnExport = new JButton("Export");
    btnExport.addActionListener(new ExportActionListener(this));
    btnExport.setEnabled(false);

    GroupLayout groupLayout = new GroupLayout(this);

    GroupLayout.ParallelGroup labels = groupLayout.createParallelGroup(LEADING)
        .addComponent(lblExportDirectory)
        .addComponent(lblPemPrivateKey)
        .addComponent(lblDateFrom)
        .addComponent(lblDateTo);

    GroupLayout.ParallelGroup fields = groupLayout.createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup().addComponent(txtExportDirectory).addPreferredGap(RELATED).addComponent(btnChooseExportDirectory))
        .addGroup(groupLayout.createSequentialGroup().addComponent(pemPrivateKeyFilePath).addPreferredGap(RELATED).addComponent(btnPemFileChooseButton))
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
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblPemPrivateKey).addComponent(pemPrivateKeyFilePath, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE).addComponent(btnPemFileChooseButton)).addPreferredGap(RELATED)
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblDateFrom).addComponent(pickStartDate))
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblDateTo).addComponent(pickEndDate))
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

  public void updateForms() {
    tableModel.setForms(FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList()));
  }

  private void chooseLocation(FileChooser fileChooser, JTextField locationField) {
    fileChooser.choose().ifPresent(file -> {
      locationField.setText(file.getAbsolutePath());
      enableExportButton();
    });
  }

  private Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::isNotEmptyNotNull)
        .map(path -> Paths.get(path).toFile());
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

  void setActiveExportState(boolean active) {
    if (active) {
      // don't allow normal actions when we are transferring...
      btnChooseExportDirectory.setEnabled(false);
      btnPemFileChooseButton.setEnabled(false);
      btnExport.setEnabled(false);
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
    } else {
      btnChooseExportDirectory.setEnabled(true);
      // touch-up with real state...
      enableExportButton();
      // retain progress text (to display last export outcome)
    }
    // remember state...
    exportStateActive = active;
  }

  void resetExport() {
  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void progress(ExportProgressEvent event) {
  }

  @EventSubscriber(eventClass = ExportProgressPercentageEvent.class)
  public void progressBar(ExportProgressPercentageEvent event) {
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void failedCompletion(ExportFailedEvent event) {
    setActiveExportState(false);
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void successfulCompletion(ExportSucceededEvent event) {
    setActiveExportState(false);
  }

  @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
  public void successfulCompletionWithErrors(ExportSucceededWithErrorsEvent event) {
    setActiveExportState(false);
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedTransferCompletion(TransferFailedEvent event) {

  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulTransferCompletion(TransferSucceededEvent event) {

  }

  @EventSubscriber(eventClass = UpdatedBriefcaseFormDefinitionEvent.class)
  public void briefcaseFormListChanges(UpdatedBriefcaseFormDefinitionEvent event) {

  }
}
