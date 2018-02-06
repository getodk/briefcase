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

public class ConfigurationPanel {
  private final ExportConfiguration configuration;
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();
  final ConfigurationPanelForm form;

  ConfigurationPanel(ExportConfiguration initialConfiguration, ConfigurationPanelForm form) {
    this.form = form;
    configuration = initialConfiguration.copy();

    configuration.ifExportDirPresent(form::setExportDir);
    configuration.ifPemFilePresent(form::setPemFile);
    configuration.ifStartDatePresent(form::setStartDate);
    configuration.ifEndDatePresent(form::setEndDate);
    configuration.ifPullBeforePresent(form::setPullBefore);
    configuration.ifPullBeforeNotPresent(form::setPullBeforeInherit);

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
    form.onChangePullBefore((pullBefore, inherit) -> {
      Boolean value = !inherit ? pullBefore : null;
      configuration.setPullBefore(value);
      triggerOnChange();
    });
  }

  public static ConfigurationPanel from(ExportConfiguration initialConfiguration, boolean isOverridePanel, boolean offerPullBefore) {
    return new ConfigurationPanel(initialConfiguration, new ConfigurationPanelForm(isOverridePanel, offerPullBefore));
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
