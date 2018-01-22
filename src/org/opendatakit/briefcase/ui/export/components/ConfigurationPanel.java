package org.opendatakit.briefcase.ui.export.components;

import java.util.ArrayList;
import java.util.List;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationPanel {
  private final ExportConfiguration configuration;
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  private final ConfigurationPanelForm form;

  ConfigurationPanel(ExportConfiguration initialConfiguration, ConfigurationPanelForm form) {
    this.form = form;
    configuration = initialConfiguration.clone();

    configuration.ifExportDirPresent(form::setExportDir);
    configuration.ifPemFilePresent(form::setPemFile);
    configuration.ifDateRangeStartPresent(form::setStartDate);
    configuration.ifDateRangeEndPresent(form::setEndDate);

    form.onSelectExportDir(path -> {
      configuration.setExportDir(path);
      triggerOnChange();
    });
    form.onSelectPemFile(path -> {
      configuration.setPemFile(path);
      triggerOnChange();
    });
    form.onSelectDateRangeStart(date -> {
      configuration.setStartDate(date);
      triggerOnChange();
    });
    form.onSelectDateRangeEnd(date -> {
      configuration.setEndDate(date);
      triggerOnChange();
    });
  }

  public static ConfigurationPanel from(ExportConfiguration config) {
    return new ConfigurationPanel(config, new ConfigurationPanelForm());
  }

  public ConfigurationPanelForm getForm() {
    return form;
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

  public void enable() {
    form.setEnabled(true);
  }

  public void disable() {
    form.setEnabled(false);
  }

  public boolean isValid() {
    return configuration.isValid();
  }

  public boolean isEmpty() {
    return configuration.isEmpty();
  }
}
