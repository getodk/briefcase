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

public class ConfigurationDialog {
  final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;

  ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel) {
    this.form = form;
    this.confPanel = confPanel;

    if (!confPanel.isEmpty())
      form.enableClearAll();

    confPanel.onChange(() -> {
      if (!confPanel.getConfiguration().isEmpty())
        form.enableClearAll();
      else
        form.disableClearAll();

      if (confPanel.getConfiguration().isValidAsCustomConf())
        form.enableOK();
      else
        form.disableOK();
    });
  }

  static ConfigurationDialog from(ExportConfiguration configuration, boolean hasTransferSettings, boolean savePasswordsConsent) {
    ConfigurationPanel confPanel = ConfigurationPanel.overridePanel(configuration, savePasswordsConsent, hasTransferSettings);
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

  public ConfigurationPanel getConfPanel() {
    return confPanel;
  }
}
