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

package org.opendatakit.briefcase.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportAbortEvent;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportProgressPercentageEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.IFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.model.UpdatedBriefcaseFormDefinitionEvent;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

import com.github.lgooddatepicker.components.DatePicker;

import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;

public class ExportPanel extends JPanel {

    private static final long serialVersionUID = 7169316129011796197L;

    static final String TAB_NAME = "Export";

    private final JTextField txtExportDirectory;

    private final JComboBox<ExportType> comboBoxExportType;

    private final JComboBox<BriefcaseFormDefinition> comboBoxForm;

    private final JButton btnChooseExportDirectory;

    private final JLabel lblExporting;
    private final JProgressBar progressBar;
    private final DetailButton btnDetails;
    private final JButton btnExport;
    private final JButton btnCancel;
    private final DatePicker pickStartDate;
    private final DatePicker pickEndDate;

    private boolean exportStateActive = false;
    private final TerminationFuture terminationFuture;

    private final StringBuilder exportStatusList;
    private final JTextField pemPrivateKeyFilePath;

    private final JButton btnPemFileChooseButton;

    class WrappedFileChooserActionListener implements ActionListener {
        private final AbstractFileChooser afc;
        private final JTextField textField;

        WrappedFileChooserActionListener(AbstractFileChooser afc, JTextField textField) {
            this.afc = afc;
            this.textField = textField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            WrappedFileChooser wfc = new WrappedFileChooser(ExportPanel.this, afc);
            String path = textField.getText();
            if (path != null && path.trim().length() != 0) {
                wfc.setSelectedFile(new File(path.trim()));
            }
            int retVal = wfc.showDialog();
            if (retVal == JFileChooser.APPROVE_OPTION && wfc.getSelectedFile() != null) {
                textField.setText(wfc.getSelectedFile().getAbsolutePath());
                resetExport();
            }
            enableExportButton(); // likely disabled...
        }
    }

    class FormSelectionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            resetExport();
            enableExportButton();
        }
    }

    public class DetailButton extends JButton implements ActionListener {

        private static final long serialVersionUID = -5106358166776020642L;

        private IFormDefinition form;
        private String dirName;
        @SuppressWarnings("unused")
        private ExportType type;

        DetailButton() {
            super(TAB_NAME + " Details...");
            this.addActionListener(this);
        }

        void setContext() {
            form = ((IFormDefinition) comboBoxForm.getSelectedItem());
            type = (ExportType) comboBoxExportType.getSelectedItem();
            File outputDir = new File(txtExportDirectory.getText());
            dirName = outputDir.getAbsolutePath();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final String history = exportStatusList.toString();
            if ( history.length() == 0 ) {
                setEnabled(false);
                return;
            }

            try {
                setEnabled(false);
                ScrollingStatusListDialog.showExportDialog(JOptionPane.getFrameForComponent(this),
                        form, dirName, history);
            } finally {
                setEnabled(true);
            }
        }
    }

    /**
     * Handle click-action for the "Export" button. Extracts the settings from
     * the UI and invokes the relevant TransferAction to actually do the work.
     */
    class ExportActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            String exportDir = txtExportDirectory.getText();
            if ( exportDir == null || exportDir.trim().length() == 0 ) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        TAB_NAME + " directory was not specified.",
                        MessageStrings.INVALID_EXPORT_DIRECTORY);
                return;
            }
            File exportDirectory = new File(exportDir.trim());
            if ( !exportDirectory.exists() ) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        MessageStrings.DIR_NOT_EXIST,
                        MessageStrings.INVALID_EXPORT_DIRECTORY);
                return;
            }
            if ( !exportDirectory.isDirectory() ) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        MessageStrings.DIR_NOT_DIRECTORY,
                        MessageStrings.INVALID_EXPORT_DIRECTORY);
                return;
            }
            if ( FileSystemUtils.isUnderODKFolder(exportDirectory) ) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY,
                        MessageStrings.INVALID_EXPORT_DIRECTORY);
                return;
            } else if ( isUnderBriefcaseFolder(exportDirectory)) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE,
                        MessageStrings.INVALID_EXPORT_DIRECTORY);
                return;
            }

            if (comboBoxExportType.getSelectedIndex() == -1 || comboBoxForm.getSelectedIndex() == -1) {
                return;
            }

            ExportType exportType = (ExportType) comboBoxExportType.getSelectedItem();
            BriefcaseFormDefinition lfd = (BriefcaseFormDefinition) comboBoxForm.getSelectedItem();

            File pemFile = null;
            if ( lfd.isFileEncryptedForm() || lfd.isFieldEncryptedForm() ) {
                pemFile = new File(pemPrivateKeyFilePath.getText());
                if ( !pemFile.exists()) {
                    ODKOptionPane.showErrorDialog(ExportPanel.this,
                            "Briefcase action failed: No PrivateKey file for encrypted form",
                            MessageStrings.ERROR_DIALOG_TITLE);
                    return;
                }
            }

            Date fromDate = pickStartDate.convert().getDateWithDefaultZone();
            Date toDate = pickEndDate.convert().getDateWithDefaultZone();
            if (fromDate != null && toDate != null && fromDate.compareTo(toDate) > 0) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        MessageStrings.INVALID_DATE_RANGE_MESSAGE,
                        MessageStrings.INVALID_DATE_RANGE_TITLE);
                return;
            }

            // OK -- launch background task to do the export

            try {
                setActiveExportState(true);
                ExportAction.export(exportDirectory, exportType, lfd, pemFile, terminationFuture, fromDate, toDate);
            } catch (IOException ex) {
                ODKOptionPane.showErrorDialog(ExportPanel.this,
                        "Briefcase action failed: " + ex.getMessage(), "Briefcase Action Failed");
                setActiveExportState(true);
            }
        }
    }

    private void enableExportButton() {
        if (comboBoxForm.getSelectedIndex() == -1) {
            btnPemFileChooseButton.setEnabled(false);
            btnExport.setEnabled(false);
            return;
        }

        BriefcaseFormDefinition lfd = (BriefcaseFormDefinition) comboBoxForm.getSelectedItem();
        if ( lfd == null ) {
            btnPemFileChooseButton.setEnabled(false);
            btnExport.setEnabled(false);
            return;
        }

        if ( lfd.isFileEncryptedForm() || lfd.isFieldEncryptedForm() ) {
            btnPemFileChooseButton.setEnabled(true);
            File pemFile = new File(pemPrivateKeyFilePath.getText());
            if ( !pemFile.exists()) {
                btnExport.setEnabled(false);
                return;
            }
        } else {
            btnPemFileChooseButton.setEnabled(false);
        }

        if (comboBoxExportType.getSelectedIndex() == -1) {
            btnExport.setEnabled(false);
            return;
        }

        String exportDir = txtExportDirectory.getText();
        if ( exportDir == null || exportDir.trim().length() == 0 ) {
            btnExport.setEnabled(false);
            return;
        }

        boolean enabled = true;
        File exportDirectory = new File(exportDir.trim());
        if (!exportDirectory.exists() ) {
            enabled = false;
        }
        if ( !exportDirectory.isDirectory() ) {
            enabled = false;
        }
        if ( FileSystemUtils.isUnderODKFolder(exportDirectory) ) {
            enabled = false;
        } else if ( isUnderBriefcaseFolder(exportDirectory)) {
            enabled = false;
        }
        btnExport.setEnabled(enabled);
    }

    ExportPanel(TerminationFuture terminationFuture) {
        super();
        AnnotationProcessor.process(this);// if not using AOP
        this.terminationFuture = terminationFuture;

        JLabel lblForm = new JLabel("Form:");
        comboBoxForm = new JComboBox<>();
        updateComboBox();
        comboBoxForm.addActionListener(new FormSelectionListener());

        JLabel lblExportType = new JLabel(TAB_NAME + " Type:");
        comboBoxExportType = new JComboBox<>(ExportType.values());

        JLabel lblExportDirectory = new JLabel(TAB_NAME + " Directory:");

        txtExportDirectory = new JTextField();
        txtExportDirectory.setFocusable(false);
        txtExportDirectory.setEditable(false);
        txtExportDirectory.setColumns(10);

        btnChooseExportDirectory = new JButton("Choose...");
        btnChooseExportDirectory.addActionListener(
                new WrappedFileChooserActionListener(new ExportFolderChooser(this), txtExportDirectory));

        JLabel lblPemPrivateKey = new JLabel("PEM Private Key File:");

        pemPrivateKeyFilePath = new JTextField();
        pemPrivateKeyFilePath.setFocusable(false);
        pemPrivateKeyFilePath.setEditable(false);
        pemPrivateKeyFilePath.setColumns(10);

        btnPemFileChooseButton = new JButton("Choose...");
        btnPemFileChooseButton.addActionListener(
                new WrappedFileChooserActionListener(new PrivateKeyFileChooser(this), pemPrivateKeyFilePath));

        JLabel lblDateFrom = new JLabel("Start Date (inclusive):");
        JLabel lblDateTo = new JLabel("End Date (exclusive):");

        pickStartDate = createDatePicker();
        pickEndDate = createDatePicker();

        lblExporting = new JLabel("");
        lblExporting.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnDetails = new DetailButton();
        btnDetails.setEnabled(false);

        btnExport = new JButton(TAB_NAME);
        btnExport.addActionListener(new ExportActionListener());
        btnExport.setEnabled(false);

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ExportPanel.this.terminationFuture.markAsCancelled(
                        new ExportAbortEvent(TAB_NAME + " cancelled by user."));
            }
        });

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(groupLayout
                .createSequentialGroup()
                .addContainerGap()
                .addGroup(
                        groupLayout
                                .createParallelGroup(Alignment.LEADING)
                                .addGroup(
                                        groupLayout
                                                .createSequentialGroup()
                                                .addGroup(
                                                        groupLayout.createParallelGroup(Alignment.LEADING)
                                                                .addComponent(lblForm)
                                                                .addComponent(lblExportType)
                                                                .addComponent(lblExportDirectory)
                                                                .addComponent(lblPemPrivateKey)
                                                                .addComponent(lblDateFrom)
                                                                .addComponent(lblDateTo))
                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                .addGroup(
                                                        groupLayout
                                                                .createParallelGroup(Alignment.LEADING)
                                                                .addComponent(comboBoxForm, Alignment.TRAILING,
                                                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                                                .addComponent(comboBoxExportType, Alignment.TRAILING,
                                                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                                                .addGroup(
                                                                        groupLayout.createSequentialGroup()
                                                                                .addComponent(txtExportDirectory)
                                                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                                                .addComponent(btnChooseExportDirectory))
                                                                .addGroup(
                                                                        groupLayout.createSequentialGroup().addComponent(pemPrivateKeyFilePath)
                                                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                                                .addComponent(btnPemFileChooseButton))
                                                                .addGap(0)
                                                                .addComponent(pickStartDate)
                                                                .addComponent(pickEndDate)))
                                .addComponent(lblExporting)
                                .addGroup(
                                        Alignment.TRAILING,
                                        groupLayout.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(progressBar).addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnDetails).addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnExport).addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnCancel))).addContainerGap());

        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(
                        groupLayout
                                .createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(comboBoxForm, GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lblForm))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(comboBoxExportType, GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lblExportType))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(lblExportDirectory)
                                                .addComponent(btnChooseExportDirectory)
                                                .addComponent(txtExportDirectory, GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(lblPemPrivateKey)
                                                .addComponent(pemPrivateKeyFilePath, GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnPemFileChooseButton))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(lblDateFrom)
                                                .addComponent(pickStartDate))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.BASELINE)
                                                .addComponent(lblDateTo)
                                                .addComponent(pickEndDate))
                                .addPreferredGap(ComponentPlacement.UNRELATED, 10, Short.MAX_VALUE)
                                .addGroup(
                                        groupLayout
                                                .createParallelGroup(Alignment.TRAILING)
                                                .addComponent(lblExporting).addComponent(btnDetails)
                                                .addComponent(progressBar).addComponent(btnDetails)
                                                .addComponent(btnExport).addComponent(btnCancel))
                                .addContainerGap()));

        exportStatusList = new StringBuilder();
        setLayout(groupLayout);
        setActiveExportState(exportStateActive);
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
        updateComboBox();

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
            comboBoxExportType.setEnabled(false);
            comboBoxForm.setEnabled(false);
            btnChooseExportDirectory.setEnabled(false);
            btnPemFileChooseButton.setEnabled(false);
            btnExport.setEnabled(false);
            // enable cancel button
            btnCancel.setEnabled(true);
            // show downloading progress bar
            progressBar.setVisible(true);
            // save the context of this export action
            lblExporting.setText("");
            btnDetails.setContext();
            // reset the export details list
            exportStatusList.setLength(0);
            exportStatusList.append("Starting " + TAB_NAME + "...");
            btnDetails.setEnabled(true);
            // reset the termination future so we can cancel activity
            terminationFuture.reset();
        } else {
            // restore normal actions when we aren't transferring...
            comboBoxExportType.setEnabled(true);
            comboBoxForm.setEnabled(true);
            btnChooseExportDirectory.setEnabled(true);
            // touch-up with real state...
            enableExportButton();
            // disable cancel button
            btnCancel.setEnabled(false);
            // retain progress text (to display last export outcome)
            // disable progress bar
            progressBar.setVisible(false);
            progressBar.setValue(0);
        }
        // remember state...
        exportStateActive = active;
    }

    private void resetExport() {
        exportStatusList.setLength(0);
        lblExporting.setText(" ");
        btnDetails.setEnabled(false);
    }

    @EventSubscriber(eventClass = ExportProgressEvent.class)
    public void progress(ExportProgressEvent event) {
        exportStatusList.append("\n").append(event.getText());
    }

    @EventSubscriber(eventClass = ExportProgressPercentageEvent.class)
    public void progressBar(ExportProgressPercentageEvent event) {
        progressBar.setValue((int)event.getProgress());
    }

    @EventSubscriber(eventClass = ExportFailedEvent.class)
    public void failedCompletion(ExportFailedEvent event) {
        exportStatusList.append("\n").append("FAILED!");
        lblExporting.setText("FAILED!");
        setActiveExportState(false);
    }

    @EventSubscriber(eventClass = ExportSucceededEvent.class)
    public void successfulCompletion(ExportSucceededEvent event) {
        exportStatusList.append("\n").append("SUCCEEDED!");
        lblExporting.setText("SUCCEEDED!");
        setActiveExportState(false);
    }

    @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
    public void successfulCompletionWithErrors(ExportSucceededWithErrorsEvent event) {
        exportStatusList.append("\n").append("SUCCEEDED, BUT WITH ERRORS!");
        lblExporting.setText("SUCCEEDED, BUT WITH ERRORS. SEE DETAILS!");
        setActiveExportState(false);
    }

    void updateComboBox() {
        final BriefcaseFormDefinition selectedForm = comboBoxForm.getSelectedIndex() == -1 ?
                null : (BriefcaseFormDefinition) comboBoxForm.getSelectedItem();
        List<BriefcaseFormDefinition> forms = FileSystemUtils.getBriefcaseFormList();
        comboBoxForm.setModel(new DefaultComboBoxModel<>(forms.toArray(new BriefcaseFormDefinition[forms.size()])));
        if ( selectedForm != null ) {
            for ( int i = 0 ; i < forms.size() ; ++i ) {
                if ( forms.get(i).equals(selectedForm) ) {
                    comboBoxForm.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    @EventSubscriber(eventClass = TransferFailedEvent.class)
    public void failedTransferCompletion(TransferFailedEvent event) {
        updateComboBox();
    }

    @EventSubscriber(eventClass = TransferSucceededEvent.class)
    public void successfulTransferCompletion(TransferSucceededEvent event) {
        updateComboBox();
    }

    @EventSubscriber(eventClass = UpdatedBriefcaseFormDefinitionEvent.class)
    public void briefcaseFormListChanges(UpdatedBriefcaseFormDefinitionEvent event) {
        updateComboBox();
    }
}
