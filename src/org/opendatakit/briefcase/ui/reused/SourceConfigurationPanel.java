package org.opendatakit.briefcase.ui.reused;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;

public class SourceConfigurationPanel {
  public static final String SELECT_SOURCE_VIEW = "select source view";
  public static final String SHOW_SOURCE_VIEW = "show source view";
  private final VirtualContainerForm container = new VirtualContainerForm();
  private final List<Consumer<AggregateServerConnectionConfiguration>> onAggregateSourceCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onCustomDirectorySourceCallbacks = new ArrayList<>();
  private final List<Runnable> onResetCallbacks = new ArrayList<>();

  public SourceConfigurationPanel() {
    SourceSelectPanelForm selectSourceView = new SourceSelectPanelForm();
    SourceDisplayPanelForm showSourceView = SourceDisplayPanelForm.empty();
    container.addForm(SELECT_SOURCE_VIEW, selectSourceView.container);
    container.addForm(SHOW_SOURCE_VIEW, showSourceView.container);

    selectSourceView.onAggregateConnection(configuration -> {
      container.navigateTo(SHOW_SOURCE_VIEW);
      showSourceView.readConfigAggregate(configuration);
      triggerAggregateConfiguredSource(configuration);
    });

    selectSourceView.onCustomDir(path -> {
      container.navigateTo(SHOW_SOURCE_VIEW);
      showSourceView.readConfigCustomDir(path);
      triggerCustomDirSource(path);
    });

    showSourceView.onReset(() -> {
      container.navigateTo(SELECT_SOURCE_VIEW);
      triggerReset();
    });

    container.navigateTo(SELECT_SOURCE_VIEW);
  }

  public void onAggregateConnection(Consumer<AggregateServerConnectionConfiguration> consumer) {
    onAggregateSourceCallbacks.add(consumer);
  }

  public void onCustomDir(Consumer<Path> consumer) {
    onCustomDirectorySourceCallbacks.add(consumer);
  }

  public void onReset(Runnable runnable) {
    onResetCallbacks.add(runnable);
  }

  private void triggerAggregateConfiguredSource(AggregateServerConnectionConfiguration conf) {
    onAggregateSourceCallbacks.forEach(callback -> callback.accept(conf));
  }

  private void triggerCustomDirSource(Path path) {
    onCustomDirectorySourceCallbacks.forEach(callback -> callback.accept(path));
  }

  private void triggerReset() {
    onResetCallbacks.forEach(Runnable::run);
  }

  public JPanel getForm() {
    return container.container;
  }

}
