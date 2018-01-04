package org.opendatakit.briefcase.ui.export;

import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.ui.MessageStrings;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

/**
 * Handle click-action for the "Export" button. Extracts the settings from
 * the UI and invokes the relevant TransferAction to actually do the work.
 */
class ExportActionListener implements ActionListener {

  private ExportPanel exportPanel;

  public ExportActionListener(ExportPanel exportPanel) {
    this.exportPanel = exportPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    String exportDir = exportPanel.txtExportDirectory.getText();
    if (exportDir == null || exportDir.trim().length() == 0) {
      ODKOptionPane.showErrorDialog(exportPanel,
          ExportPanel.TAB_NAME + " directory was not specified.",
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return;
    }
    File exportDirectory = new File(exportDir.trim());
    if (!exportDirectory.exists()) {
      ODKOptionPane.showErrorDialog(exportPanel,
          MessageStrings.DIR_NOT_EXIST,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return;
    }
    if (!exportDirectory.isDirectory()) {
      ODKOptionPane.showErrorDialog(exportPanel,
          MessageStrings.DIR_NOT_DIRECTORY,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return;
    }
    if (FileSystemUtils.isUnderODKFolder(exportDirectory)) {
      ODKOptionPane.showErrorDialog(exportPanel,
          MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return;
    } else if (isUnderBriefcaseFolder(exportDirectory)) {
      ODKOptionPane.showErrorDialog(exportPanel,
          MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return;
    }

    if (exportPanel.comboBoxExportType.getSelectedIndex() == -1 || exportPanel.comboBoxForm.getSelectedIndex() == -1) {
      return;
    }

    ExportType exportType = (ExportType) exportPanel.comboBoxExportType.getSelectedItem();
    BriefcaseFormDefinition lfd = (BriefcaseFormDefinition) exportPanel.comboBoxForm.getSelectedItem();

    File pemFile = null;
    if (lfd.isFileEncryptedForm() || lfd.isFieldEncryptedForm()) {
      pemFile = new File(exportPanel.pemPrivateKeyFilePath.getText());
      if (!pemFile.exists()) {
        ODKOptionPane.showErrorDialog(exportPanel,
            "Briefcase action failed: No PrivateKey file for encrypted form",
            MessageStrings.ERROR_DIALOG_TITLE);
        return;
      }
    }

    Date fromDate = exportPanel.pickStartDate.convert().getDateWithDefaultZone();
    Date toDate = exportPanel.pickEndDate.convert().getDateWithDefaultZone();
    if (fromDate != null && toDate != null && fromDate.compareTo(toDate) > 0) {
      ODKOptionPane.showErrorDialog(exportPanel,
          MessageStrings.INVALID_DATE_RANGE_MESSAGE,
          MessageStrings.INVALID_DATE_RANGE_TITLE);
      return;
    }

    // OK -- launch background task to do the export

    try {
      exportPanel.setActiveExportState(true);
      ExportAction.export(exportDirectory, exportType, lfd, pemFile, exportPanel.terminationFuture, fromDate, toDate);
    } catch (IOException ex) {
      ODKOptionPane.showErrorDialog(exportPanel,
          "Briefcase action failed: " + ex.getMessage(), "Briefcase Action Failed");
      exportPanel.setActiveExportState(true);
    }
  }
}
