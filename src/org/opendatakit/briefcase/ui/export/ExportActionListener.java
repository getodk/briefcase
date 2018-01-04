package org.opendatakit.briefcase.ui.export;

import static java.util.stream.Collectors.toList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.util.ExportAction;

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
    List<String> configurationErrors = exportPanel.getErrors();
    exportPanel.showErrors(
        configurationErrors,
        "We can't export the forms until these errors are corrected:",
        "Export configuration error"
    );
    if (configurationErrors.isEmpty()) {
      new Thread(() -> {
        exportPanel.btnExport.setEnabled(false);
        List<String> errors = export();
        exportPanel.showErrors(
            errors,
            "We have found some errors while performing the requested export actions:",
            "Export error report"
        );
        exportPanel.btnExport.setEnabled(true);
      }).start();
    }

  }

  private List<String> export() {
    return exportPanel.tableModel.getSelectedForms().parallelStream()
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(formDefinition).stream())
        .collect(toList());
  }

  private List<String> export(BriefcaseFormDefinition formDefinition) {

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    List<String> errors = new ArrayList<>();
    Optional<File> pemFile = Optional.ofNullable(exportPanel.pemPrivateKeyFilePath.getText()).map(File::new).filter(File::exists);
    if ((formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) && !pemFile.isPresent())
      errors.add(formDefinition.getFormName() + " form requires is encrypted and you haven't defined a valid private key file location");
    else
      try {
        ExportAction.export(
            new File(exportPanel.txtExportDirectory.getText().trim()),
            ExportType.CSV,
            formDefinition,
            pemFile.orElse(null),
            exportPanel.terminationFuture,
            exportPanel.pickStartDate.convert().getDateWithDefaultZone(),
            exportPanel.pickEndDate.convert().getDateWithDefaultZone()
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }
}
