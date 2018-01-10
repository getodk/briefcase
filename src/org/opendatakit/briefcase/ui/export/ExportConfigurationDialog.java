package org.opendatakit.briefcase.ui.export;

import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;

import java.awt.Window;
import java.util.function.Consumer;

public class ExportConfigurationDialog {
  private final ExportConfiguration config;
  private final ExportConfigurationDialogView view;

  private ExportConfigurationDialog(ExportConfiguration config, ExportConfigurationDialogView view, Runnable onRemove, Consumer<ExportConfiguration> onApply) {
    this.config = config;
    this.view = view;

    config.ifExportDirPresent(view::setExportDir);
    view.onSelectExportDir(path -> {
      config.setExportDir(path);
      updateAcceptButton();
    });
    config.ifPemFilePresent(view::setPemFile);
    view.onSelectPemFile(path -> {
      config.setPemFile(path);
      updateAcceptButton();
    });
    config.ifDateRangeStartPresent(view::setDateRangeStart);
    view.onSelectDateRangeStart(date -> {
      config.setDateRangeStart(date);
      if (!config.isDateRangeValid()) {
        view.showError(INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        view.clearDateRangeStart();
      }
      updateAcceptButton();
    });
    config.ifDateRangeEndPresent(view::setDateRangeEnd);
    view.onSelectDateRangeEnd(date -> {
      config.setDateRangeEnd(date);
      if (!config.isDateRangeValid()) {
        view.showError(INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        view.clearDateRangeEnd();
      }
      updateAcceptButton();
    });
    view.onClickRemoveConfig(() -> {
      onRemove.run();
      view.closeDialog();
    });
    view.onClickApplyConfig(() -> {
      onApply.accept(config);
      view.closeDialog();
    });
  }

  public static ExportConfigurationDialog from(Window app, ExportConfiguration config, Runnable onRemove, Consumer<ExportConfiguration> onApply) {
    return new ExportConfigurationDialog(
        config,
        new ExportConfigurationDialogView(app),
        onRemove,
        onApply
    );
  }

  private void updateAcceptButton() {
    if (config.isValid())
      view.enableApplyConfig();
    else
      view.disableApplyConfig();
  }


  public void open() {
    view.open();
  }
}
