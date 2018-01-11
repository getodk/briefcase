package org.opendatakit.briefcase.ui.export.components;

import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;

import java.util.ArrayList;
import java.util.List;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ConfigurationPanel {
  private final ExportConfiguration configuration;
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final ConfigurationPanelView view;

  ConfigurationPanel(ExportConfiguration initialConfiguration, ConfigurationPanelView view) {
    this.view = view;
    configuration = ExportConfiguration.copy(initialConfiguration);

    configuration.ifExportDirPresent(view::setExportDir);
    configuration.ifPemFilePresent(view::setPemFile);
    configuration.ifDateRangeStartPresent(view::setDateRangeStart);
    configuration.ifDateRangeEndPresent(view::setDateRangeEnd);

    view.onSelectExportDir(path -> {
      configuration.setExportDir(path);
      triggerOnChange();
    });
    view.onSelectPemFile(path -> {
      configuration.setPemFile(path);
      triggerOnChange();
    });
    view.onSelectDateRangeStart(date -> {
      configuration.setDateRangeStart(date);
      if (!configuration.isDateRangeValid()) {
        view.showError(INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        view.clearDateRangeStart();
        configuration.clearDateRangeStart();
      } else
        triggerOnChange();
    });
    view.onSelectDateRangeEnd(date -> {
      configuration.setDateRangeEnd(date);
      if (!configuration.isDateRangeValid()) {
        view.showError(INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        view.clearDateRangeEnd();
        configuration.clearDateRangeEnd();
      } else
        triggerOnChange();
    });
  }

  public static ConfigurationPanel from(ExportConfiguration config) {
    return new ConfigurationPanel(config, new ConfigurationPanelView());
  }

  public ConfigurationPanelView getView() {
    return view;
  }

  public ExportConfiguration getConfiguration() {
    return configuration;
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  private void triggerOnChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  public void disable() {
    view.setEnabled(false);
  }

  public void enable() {
    view.setEnabled(true);
  }

  public boolean isValid() {
    return configuration.isValid();
  }
}
