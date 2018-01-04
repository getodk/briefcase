package org.opendatakit.briefcase.ui.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class FormSelectionListener implements ActionListener {

  private ExportPanel exportPanel;

  public FormSelectionListener(ExportPanel exportPanel) {
    this.exportPanel = exportPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    exportPanel.resetExport();
    exportPanel.enableExportButton();
  }
}
