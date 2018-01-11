package org.opendatakit.briefcase.ui.export.components;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.JDialog;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

class ConfigurationDialogView extends JDialog {
  private final ConfigurationPanel configurationComponent;
  private final ConfigurationDialogBottomPanel configurationDialogBottomPanel;

  ConfigurationDialogView(Window app, ExportConfiguration config) {
    super(app, "Form export configuration", ModalityType.DOCUMENT_MODAL);
    setBounds(100, 100, 450, 250);
    getContentPane().setLayout(new BorderLayout());
    configurationComponent = ConfigurationPanel.from(config);
    configurationComponent.onChange(this::updateApplyConfigButton);

    configurationDialogBottomPanel = new ConfigurationDialogBottomPanel(configurationComponent);
    configurationDialogBottomPanel.onCancel(this::closeDialog);

    if (config.isEmpty())
      configurationDialogBottomPanel.disableRemove();

    getRootPane().setDefaultButton(configurationDialogBottomPanel.getDefaultButton());
    setLayout(new BorderLayout());
    getContentPane().add(configurationComponent.getView(), BorderLayout.NORTH);
    getContentPane().add(configurationDialogBottomPanel, BorderLayout.SOUTH);
    updateApplyConfigButton();
  }

  void closeDialog() {
    setVisible(false);
  }


  public void onRemove(Runnable callback) {
    configurationDialogBottomPanel.onRemove(callback);
  }

  public void onApply(Consumer<ExportConfiguration> callback) {
    configurationDialogBottomPanel.onApply(callback);
  }

  public void open() {
    setVisible(true);
  }

  private void updateApplyConfigButton() {
    if (configurationComponent.getConfiguration().isValid())
      configurationDialogBottomPanel.enableApply();
    else
      configurationDialogBottomPanel.disableApply();
  }
}
