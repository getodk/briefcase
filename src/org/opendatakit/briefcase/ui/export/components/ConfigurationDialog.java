package org.opendatakit.briefcase.ui.export.components;

import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ConfigurationDialog {
  private final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;

  private ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel) {
    this.form = form;
    this.confPanel = confPanel;

    if (!confPanel.isEmpty())
      form.enableRemove();
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
