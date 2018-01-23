package org.opendatakit.briefcase.ui.export.components;

import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationDialog {
  final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;

  ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel) {
    this.form = form;
    this.confPanel = confPanel;

    if (!confPanel.isEmpty())
      form.enableRemove();

    confPanel.onChange(() -> {
      if (this.confPanel.getConfiguration().isValidAsCustomConf())
        form.enableOK();
      else
        form.disableOK();
    });
  }

  static ConfigurationDialog from(Optional<ExportConfiguration> configuration) {
    ConfigurationPanel confPanel = ConfigurationPanel.from(configuration.orElse(ExportConfiguration.empty()), true);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel.getForm());
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
