package org.opendatakit.briefcase.ui.export.components;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.JDialog;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

class ConfigurationDialog extends JDialog {
  private final ConfigurationPanel confPanel;
  private final ConfigurationDialogBottomPanel bottomPanel;

  ConfigurationDialog(Window app, ExportConfiguration config) {
    super(app, "Form export configuration", ModalityType.DOCUMENT_MODAL);
    setBounds(100, 100, 450, 250);
    getContentPane().setLayout(new BorderLayout());
    confPanel = ConfigurationPanel.from(config);
    confPanel.onChange(this::updateApplyConfigButton);

    bottomPanel = new ConfigurationDialogBottomPanel(confPanel);
    bottomPanel.onRemove(this::closeDialog);
    bottomPanel.onApply(__ -> this.closeDialog());
    bottomPanel.onCancel(this::closeDialog);

    if (config.isEmpty())
      bottomPanel.disableRemove();

    getRootPane().setDefaultButton(bottomPanel.getDefaultButton());
    setLayout(new BorderLayout());
    getContentPane().add(confPanel.getView(), BorderLayout.NORTH);
    getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    updateApplyConfigButton();
  }

  private void closeDialog() {
    setVisible(false);
  }


  public void onRemove(Runnable callback) {
    bottomPanel.onRemove(callback);
  }

  public void onApply(Consumer<ExportConfiguration> callback) {
    bottomPanel.onApply(callback);
  }

  public void open() {
    setVisible(true);
  }

  private void updateApplyConfigButton() {
    if (confPanel.getConfiguration().isValid())
      bottomPanel.enableApply();
    else
      bottomPanel.disableApply();
  }
}
