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
package org.opendatakit.briefcase.delivery.ui.export.components;

import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;

public class ConfigurationDialog {
  final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;
  // Hold the configuration for the onOk callback
  private ExportConfiguration configuration;

  private ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel, ExportConfiguration configuration, Function<ExportConfiguration, Boolean> confValidator) {
    this.form = form;
    this.confPanel = confPanel;
    this.configuration = configuration;

    confPanel.onChange(conf -> {
      this.configuration = conf;
      if (conf.isEmpty())
        form.disableClearAll();
      else
        form.enableClearAll();

      if (confValidator.apply(conf))
        form.enableOK();
      else
        form.disableOK();
    });
  }

  static ConfigurationDialog overridePanel(ExportConfiguration initialConfiguration, String formName, boolean hasTransferSettings, boolean savePasswordsConsent) {
    ConfigurationPanel confPanel = ConfigurationPanel.overridePanel(initialConfiguration, savePasswordsConsent, hasTransferSettings);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel.getForm(), "Override " + formName + " Export Configuration");
    if (initialConfiguration.isEmpty())
      form.disableClearAll();
    else
      form.enableClearAll();

    form.enableOK();

    return new ConfigurationDialog(form, confPanel, initialConfiguration, configuration1 -> true);
  }

  public static ConfigurationDialog defaultPanel(ExportConfiguration initialConfiguration, boolean savePasswordsConsent) {
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(initialConfiguration, savePasswordsConsent);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel.getForm(), "Default Export Configuration");
    if (!initialConfiguration.isEmpty())
      form.enableClearAll();

    if (!initialConfiguration.isValid())
      form.disableOK();
    return new ConfigurationDialog(form, confPanel, initialConfiguration, ExportConfiguration::isValid);
  }

  public void onOK(Consumer<ExportConfiguration> callback) {
    form.onOK(() -> callback.accept(configuration));
  }

  public void onRemove(Runnable callback) {
    form.onRemove(callback);
  }

  public void open() {
    form.open();
  }

  ConfigurationPanel getConfPanel() {
    return confPanel;
  }
}
