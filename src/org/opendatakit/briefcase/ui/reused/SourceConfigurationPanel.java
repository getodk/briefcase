package org.opendatakit.briefcase.ui.reused;

public class SourceConfigurationPanel {
  private final SourceConfigurationPanelForm form;
  private SourceDisplayPanelForm formSourceDisplay;

  public SourceConfigurationPanel(SourceConfigurationPanelForm form) {
    this.form = form;
  }

  public SourceConfigurationPanelForm getFormSourceConfig() {
    return form;
  }

  public void setFormSourceDisplay(SourceDisplayPanelForm sourceDisplay) {
    formSourceDisplay = sourceDisplay;
  }

  public SourceDisplayPanelForm getFormSourceDisplay() {
    return formSourceDisplay;
  }
}
