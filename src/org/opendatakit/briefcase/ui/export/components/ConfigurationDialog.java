package org.opendatakit.briefcase.ui.export.components;

import java.util.function.Consumer;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ConfigurationDialog {
  private final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;

  private ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel) {
    this.form = form;
    this.confPanel = confPanel;

    confPanel.onChange(() -> {
      if (this.confPanel.getConfiguration().isValid())
        form.enableOK();
      else
        form.disableOK();
    });
  }

  static ConfigurationDialog from(ExportConfiguration configuration) {
    ConfigurationPanel confPanel = ConfigurationPanel.from(configuration);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel);
    form.setBounds(100, 100, 450, 250);
    return new ConfigurationDialog(form, confPanel);
  }

  void onOK(Consumer<ExportConfiguration> callback) {
    form.onOK(() -> callback.accept(confPanel.getConfiguration()));
  }

  public void onRemove(Runnable callback) {
    form.onRemove(callback);
  }

  public void open() {
    form.open();
  }
}
