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

class ConfigurationPanelMode {
  static final String REQUIRE_PULL_TEXT = "Requires manually pulling from Aggregate once";
  static final String REQUIRE_SAVE_PASSWORDS = "Requires Remember passwords in Settings";
  private final boolean isOverridePanel;
  private boolean savePasswordsConsent;
  private final boolean hasTransferSettings;

  private ConfigurationPanelMode(boolean isOverridePanel, boolean savePasswordsConsent, boolean hasTransferSettings) {
    this.isOverridePanel = isOverridePanel;
    this.savePasswordsConsent = savePasswordsConsent;
    this.hasTransferSettings = hasTransferSettings;
  }

  static ConfigurationPanelMode overridePanel(boolean savePasswordsConsent, boolean hasTransferSettings) {
    return new ConfigurationPanelMode(true, savePasswordsConsent, hasTransferSettings);
  }

  static ConfigurationPanelMode defaultPanel(boolean savePasswordsConsent) {
    return new ConfigurationPanelMode(false, savePasswordsConsent, false);
  }

  void decorate(ConfigurationPanelForm form, boolean uiLocked) {
    form.pullBeforeField.setVisible(!isOverridePanel);
    form.pullBeforeField.setEnabled(!uiLocked && savePasswordsConsent && (!isOverridePanel || hasTransferSettings));
    form.pullBeforeOverrideLabel.setVisible(isOverridePanel);
    form.pullBeforeOverrideField.setVisible(isOverridePanel);
    form.pullBeforeOverrideField.setEnabled(!uiLocked && savePasswordsConsent && hasTransferSettings);
    form.pullBeforeHintPanel.setVisible(!savePasswordsConsent || !hasTransferSettings || !isOverridePanel);
    form.pullBeforeHintPanel.setText(savePasswordsConsent
        ? hasTransferSettings && isOverridePanel
        ? ""
        : REQUIRE_PULL_TEXT
        : REQUIRE_SAVE_PASSWORDS
    );
    form.exportMediaField.setVisible(!isOverridePanel);
    form.exportMediaOverrideField.setVisible(isOverridePanel);
    form.exportMediaOverrideField.setVisible(isOverridePanel);
    form.exportMediaOverrideLabel.setVisible(isOverridePanel);

    form.overwriteFilesField.setVisible(!isOverridePanel);
    form.overwriteFilesOverrideLabel.setVisible(isOverridePanel);
    form.overwriteFilesOverrideField.setVisible(isOverridePanel);

    form.splitSelectMultiplesField.setVisible(!isOverridePanel);
    form.splitSelectMultiplesOverrideLabel.setVisible(isOverridePanel);
    form.splitSelectMultiplesOverrideField.setVisible(isOverridePanel);

    form.includeGeoJsonExportField.setVisible(!isOverridePanel);
    form.includeGeoJsonExportOverrideLabel.setVisible(isOverridePanel);
    form.includeGeoJsonExportOverrideField.setVisible(isOverridePanel);

    form.removeGroupNamesField.setVisible(!isOverridePanel);
    form.removeGroupNamesOverrideLabel.setVisible(isOverridePanel);
    form.removeGroupNamesOverrideField.setVisible(isOverridePanel);
  }

  boolean isExportDirCleanable() {
    return isOverridePanel;
  }

  void setSavePasswordsConsent(boolean savePasswordsConsent) {
    this.savePasswordsConsent = savePasswordsConsent;
  }
}
