package org.opendatakit.briefcase.ui.export.components;

import java.awt.Window;
import java.util.function.Consumer;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

class ConfigurationDialog {
  private final ConfigurationDialogView view;

  ConfigurationDialog(Window app, ExportConfiguration config) {
    view = new ConfigurationDialogView(app, config);
    view.onRemove(view::closeDialog);
    view.onApply(__ -> view.closeDialog());
  }

  public void onRemove(Runnable callback) {
    this.view.onRemove(callback);
  }

  public void onApply(Consumer<ExportConfiguration> callback) {
    this.view.onApply(callback);
  }

  public void open() {
    view.open();
  }
}
