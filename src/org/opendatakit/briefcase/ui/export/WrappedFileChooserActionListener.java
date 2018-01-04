package org.opendatakit.briefcase.ui.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import org.opendatakit.briefcase.ui.AbstractFileChooser;
import org.opendatakit.briefcase.ui.WrappedFileChooser;

class WrappedFileChooserActionListener implements ActionListener {
  private ExportPanel exportPanel;
  private final AbstractFileChooser afc;
  private final JTextField textField;

  WrappedFileChooserActionListener(ExportPanel exportPanel, AbstractFileChooser afc, JTextField textField) {
    this.exportPanel = exportPanel;
    this.afc = afc;
    this.textField = textField;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    WrappedFileChooser wfc = new WrappedFileChooser(exportPanel, afc);
    String path = textField.getText();
    if (path != null && path.trim().length() != 0) {
      wfc.setSelectedFile(new File(path.trim()));
    }
    int retVal = wfc.showDialog();
    if (retVal == JFileChooser.APPROVE_OPTION && wfc.getSelectedFile() != null) {
      textField.setText(wfc.getSelectedFile().getAbsolutePath());
      exportPanel.resetExport();
    }
    exportPanel.enableExportButton(); // likely disabled...
  }
}
