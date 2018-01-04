package org.opendatakit.briefcase.ui.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.IFormDefinition;
import org.opendatakit.briefcase.ui.ScrollingStatusListDialog;

public class DetailButton extends JButton implements ActionListener {

  private static final long serialVersionUID = -5106358166776020642L;

  private ExportPanel exportPanel;
  private IFormDefinition form;
  private String dirName;
  @SuppressWarnings("unused")
  private ExportType type;

  DetailButton(ExportPanel exportPanel) {
    super(ExportPanel.TAB_NAME + " Details...");
    this.exportPanel = exportPanel;
    this.addActionListener(this);
  }

  void setContext() {
    form = ((IFormDefinition) exportPanel.comboBoxForm.getSelectedItem());
    type = (ExportType) exportPanel.comboBoxExportType.getSelectedItem();
    File outputDir = new File(exportPanel.txtExportDirectory.getText());
    dirName = outputDir.getAbsolutePath();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final String history = exportPanel.exportStatusList.toString();
    if (history.length() == 0) {
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
