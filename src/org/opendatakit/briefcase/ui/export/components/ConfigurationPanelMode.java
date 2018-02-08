package org.opendatakit.briefcase.ui.export.components;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextPane;

class ConfigurationPanelMode {
  private final boolean isOverridePanel;
  private final boolean hasTransferSettings;
  private boolean savePasswordsConsent;

  private ConfigurationPanelMode(boolean isOverridePanel, boolean hasTransferSettings, boolean savePasswordsConsent) {
    this.isOverridePanel = isOverridePanel;
    this.hasTransferSettings = hasTransferSettings;
    this.savePasswordsConsent = savePasswordsConsent;
  }

  static ConfigurationPanelMode overridePanel(boolean hasTransferSettings, boolean savePasswordsConsent) {
    return new ConfigurationPanelMode(true, hasTransferSettings, savePasswordsConsent);
  }

  static ConfigurationPanelMode defaultPanel(boolean savePasswordsConsent) {
    return new ConfigurationPanelMode(false, true, savePasswordsConsent);
  }

  void decorate(JCheckBox pullBeforeField, JLabel pullBeforeOverrideLabel, JComboBox pullBeforeOverrideField, JTextPane textpanel) {
    pullBeforeField.setText(savePasswordsConsent ? pullBeforeField.getText() : pullBeforeField.getText() + " (Requires Remember passwords in Settings)");
    pullBeforeField.setVisible(!isOverridePanel);
    pullBeforeField.setEnabled(savePasswordsConsent && hasTransferSettings);
    pullBeforeOverrideLabel.setVisible(isOverridePanel);
    pullBeforeOverrideField.setVisible(isOverridePanel);
    pullBeforeOverrideField.setEnabled(savePasswordsConsent && hasTransferSettings);
    textpanel.setVisible(isOverridePanel && (!savePasswordsConsent || !hasTransferSettings));
    textpanel.setText(!savePasswordsConsent
        ? "You can't pull before exporting forms until you give your consent to store passwords on the Settings panel"
        : !hasTransferSettings
        ? "Please, pull this form once again to be able to check these checkboxes"
        : "");

  }

  boolean isExportDirCleanable() {
    return isOverridePanel;
  }

  void setSavePasswordsConsent(boolean savePasswordsConsent) {
    this.savePasswordsConsent = savePasswordsConsent;
  }
}
