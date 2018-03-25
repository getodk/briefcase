package org.opendatakit.briefcase.ui.reused;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;

public class SourceConfigurationPanel {
  public static final String SELECT_SOURCE_VIEW = "select source view";
  public static final String SHOW_SOURCE_VIEW = "show source view";
  private final VirtualContainerForm container = new VirtualContainerForm();
  private final List<Consumer<AggregateServerConnectionConfiguration>> onConfiguredSourceCallbacks = new ArrayList<>();

  public SourceConfigurationPanel() {
    SourceSelectPanelForm selectSourceView = new SourceSelectPanelForm();
    SourceDisplayPanelForm showSourceView = SourceDisplayPanelForm.empty();
    container.addForm(SELECT_SOURCE_VIEW, selectSourceView.container);
    container.addForm(SHOW_SOURCE_VIEW, showSourceView.container);

    selectSourceView.onConfiguredSource(configuration -> {
      container.navigateTo(SHOW_SOURCE_VIEW);
      showSourceView.readConfiguration(configuration);
      triggerConfiguredSource(configuration);
    });

    showSourceView.onReset(() -> container.navigateTo(SELECT_SOURCE_VIEW));

    container.navigateTo(SELECT_SOURCE_VIEW);
  }

  public void onConfiguredSource(Consumer<AggregateServerConnectionConfiguration> consumer) {
    onConfiguredSourceCallbacks.add(consumer);
  }

  private void triggerConfiguredSource(AggregateServerConnectionConfiguration conf) {
    onConfiguredSourceCallbacks.forEach(callback -> callback.accept(conf));
  }

  public JPanel getForm() {
    return container.container;
  }

}
