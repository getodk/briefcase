package org.opendatakit.briefcase.ui.export.components;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class BottomPanel extends JPanel {
  private final JButton cancel;
  private final JButton remove;
  public final JButton apply;
  private final List<Runnable> onCancelCallbacks = new ArrayList<>();
  private final List<Runnable> onRemoveCallbacks = new ArrayList<>();
  private final List<Consumer<ExportConfiguration>> onApplyCallbacks = new ArrayList<>();

  public BottomPanel(ConfigurationPanel configurationComponent) {
    super();
    remove = new JButton("Remove");
    remove.setActionCommand("Remove");
    remove.addActionListener(__ -> onRemoveCallbacks.forEach(Runnable::run));

    apply = new JButton("Apply");
    apply.setActionCommand("Apply");
    apply.addActionListener(__ -> onApplyCallbacks.forEach(callback -> callback.accept(configurationComponent.getConfiguration())));

    cancel = new JButton("Cancel");
    cancel.setActionCommand("Cancel");
    cancel.addActionListener(__ -> onCancelCallbacks.forEach(Runnable::run));

    JPanel left = new JPanel();
    left.add(cancel);

    JPanel right = new JPanel();
    right.add(remove);
    right.add(apply);

    setLayout(new BorderLayout());
    add(left, BorderLayout.WEST);
    add(right, BorderLayout.EAST);
  }

  void enableRemove() {
    remove.setEnabled(true);
  }

  public void disableRemove() {
    remove.setEnabled(false);
  }

  public void enableApply() {
    apply.setEnabled(true);
  }

  public void disableApply() {
    apply.setEnabled(false);
  }

  public void onCancel(Runnable callback) {
    onCancelCallbacks.add(callback);
  }

  public void onRemove(Runnable callback) {
    onRemoveCallbacks.add(callback);
  }

  public void onApply(Consumer<ExportConfiguration> callback) {
    onApplyCallbacks.add(callback);
  }

}
