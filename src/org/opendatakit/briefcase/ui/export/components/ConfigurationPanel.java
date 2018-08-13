/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.ui.export.components;

import java.util.ArrayList;
import java.util.List;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.reused.OverridableBoolean;

public class ConfigurationPanel {
  private final ExportConfiguration configuration;
  final ConfigurationPanelForm form;
  private final ConfigurationPanelMode mode;
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();

  ConfigurationPanel(ExportConfiguration initialConfiguration, ConfigurationPanelForm form, ConfigurationPanelMode mode) {
    this.configuration = initialConfiguration.copy();
    this.form = form;
    this.mode = mode;

    configuration.ifExportDirPresent(form::setExportDir);
    configuration.ifPemFilePresent(form::setPemFile);
    configuration.ifStartDatePresent(form::setStartDate);
    configuration.ifEndDatePresent(form::setEndDate);
    form.setPullBefore(configuration.pullBefore);
    form.setOverwriteFiles(configuration.overwriteFiles);
    form.setExportMedia(configuration.exportMedia);

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
    form.onChangePullBefore(value -> {
      configuration.pullBefore.set(value);
      triggerOnChange();
    });
    form.onChangePullBeforeOverride(value -> {
      configuration.pullBefore.overrideWith(value);
      triggerOnChange();
    });
    form.onChangeOverwriteExistingFiles(value -> {
      configuration.overwriteFiles.set(value);
      triggerOnChange();
    });
    form.onChangeOverwriteFilesOverride(value -> {
      configuration.overwriteFiles.overrideWith(value);
      triggerOnChange();
    });
    form.onChangeExportMedia(value -> {
      configuration.exportMedia.set(value);
      triggerOnChange();
    });
    form.onChangeExportMediaOverride(value -> {
      configuration.exportMedia.overrideWith(value);
      triggerOnChange();
    });
  }

  public static ConfigurationPanel overridePanel(ExportConfiguration initialConfiguration, boolean savePasswordsConsent, boolean hasTransferSettings) {
    ConfigurationPanelMode mode = ConfigurationPanelMode.overridePanel(savePasswordsConsent, hasTransferSettings);
    return new ConfigurationPanel(
        initialConfiguration,
        ConfigurationPanelForm.from(mode),
        mode
    );
  }

  public static ConfigurationPanel defaultPanel(ExportConfiguration initialConfiguration, boolean savePasswordsConsent) {
    ConfigurationPanelMode mode = ConfigurationPanelMode.defaultPanel(savePasswordsConsent);
    return new ConfigurationPanel(
        initialConfiguration,
        ConfigurationPanelForm.from(mode),
        mode
    );
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

  public void setEnabled(boolean enabled, boolean savePasswordsConsent) {
    form.setEnabled(enabled);
    form.changeMode(savePasswordsConsent);
  }

  public boolean isValid() {
    return mode.isOverridePanel()
        ? configuration.isValidAsCustomConf()
        : configuration.isValid();
  }

  public boolean isEmpty() {
    return configuration.isEmpty();
  }

  public void savePasswordsConsentGiven() {
    form.changeMode(true);
  }

  public void savePasswordsConsentRevoked() {
    form.setPullBefore(OverridableBoolean.empty());
    form.changeMode(false);
  }
}
