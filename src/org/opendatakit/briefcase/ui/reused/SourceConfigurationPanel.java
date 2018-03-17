package org.opendatakit.briefcase.ui.reused;

import javax.swing.JPanel;

public class SourceConfigurationPanel {
  private final SourceConfigurationPanelForm form;

  public SourceConfigurationPanel(SourceConfigurationPanelForm form) {
    this.form = form;
  }

  public JPanel getForm() {
    return form.container;
  }

}
