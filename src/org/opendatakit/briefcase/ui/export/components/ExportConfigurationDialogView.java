package org.opendatakit.briefcase.ui.export.components;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.JDialog;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ExportConfigurationDialogView extends JDialog {
  private final ConfigurationPanel configurationComponent;
  private final BottomPanel bottomPanel;

  ExportConfigurationDialogView(Window app, ExportConfiguration config) {
    super(app, "Form export configuration", ModalityType.DOCUMENT_MODAL);
    setBounds(100, 100, 450, 250);
    getContentPane().setLayout(new BorderLayout());
    configurationComponent = ConfigurationPanel.from(config);
    configurationComponent.onChange(this::updateApplyConfigButton);

    bottomPanel = new BottomPanel(configurationComponent);
    bottomPanel.onCancel(this::closeDialog);

    if (config.isEmpty())
      bottomPanel.disableRemove();

    getRootPane().setDefaultButton(bottomPanel.apply);
    setLayout(new BorderLayout());
    getContentPane().add(configurationComponent.getView(), BorderLayout.NORTH);
    getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    updateApplyConfigButton();
  }

  void closeDialog() {
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
    if (configurationComponent.getConfiguration().isValid())
      bottomPanel.enableApply();
    else
      bottomPanel.disableApply();
  }
}
