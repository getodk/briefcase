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

import java.util.function.Consumer;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationPanel {
  private final ConfigurationPanelForm form;
  private final ConfigurationPanelMode mode;

  private ConfigurationPanel(ExportConfiguration initialConfiguration, ConfigurationPanelForm form, ConfigurationPanelMode mode) {
    this.form = form;
    this.mode = mode;

    form.initialize(initialConfiguration);
  }

  static ConfigurationPanel overridePanel(ExportConfiguration initialConfiguration, boolean savePasswordsConsent, boolean hasTransferSettings) {
    ConfigurationPanelMode mode = ConfigurationPanelMode.overridePanel(savePasswordsConsent, hasTransferSettings);
    return new ConfigurationPanel(
        initialConfiguration,
        new ConfigurationPanelForm(mode),
        mode
    );
  }

  static ConfigurationPanel defaultPanel(ExportConfiguration initialConfiguration, boolean savePasswordsConsent) {
    ConfigurationPanelMode mode = ConfigurationPanelMode.defaultPanel(savePasswordsConsent);
    return new ConfigurationPanel(
        initialConfiguration,
        new ConfigurationPanelForm(mode),
        mode
    );
  }

  public ConfigurationPanelForm getForm() {
    return form;
  }

  public void onChange(Consumer<ExportConfiguration> callback) {
    form.onChange(callback);
  }

  public void setEnabled(boolean enabled, boolean savePasswordsConsent) {
    form.setEnabled(enabled);
    form.changeMode(savePasswordsConsent);
  }

  public boolean isEmpty() {
    return form.getConfiguration().isEmpty();
  }

  boolean isOverridePanel() {
    return mode.isOverridePanel();
  }

}
