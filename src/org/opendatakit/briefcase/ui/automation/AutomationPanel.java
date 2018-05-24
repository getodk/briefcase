package org.opendatakit.briefcase.ui.automation;

import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;

public class AutomationPanel {
  public static final String TAB_NAME = "Automation";

  private final AutomationPanelForm view;


  public AutomationPanel(AutomationPanelForm view) {
    AnnotationProcessor.process(this);
    this.view = view;
  }

  public static AutomationPanel from() {
    return new AutomationPanel(
        new AutomationPanelForm()
    );
  }

  public JPanel getContainer() {
    return view.container;
  }
}
