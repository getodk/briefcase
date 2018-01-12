package org.opendatakit.briefcase.ui.export.components;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ExportPanelBottomPanel extends JPanel {
  private final JButton btnSelectAll;
  private final JButton btnClearAll;
  private final JButton btnExport;
  private final List<Runnable> onSelectAllCallbacks = new ArrayList<>();
  private final List<Runnable> onClearAllCallbacks = new ArrayList<>();
  private final List<Runnable> onExportCallbacks = new ArrayList<>();

  public ExportPanelBottomPanel() {
    btnSelectAll = new JButton("Select all");
    btnSelectAll.addActionListener(__ -> onSelectAllCallbacks.forEach(Runnable::run));

    btnClearAll = new JButton("Clear all");
    btnClearAll.setVisible(false);
    btnClearAll.addActionListener(__ -> onClearAllCallbacks.forEach(Runnable::run));

    btnExport = new JButton("Export");
    btnExport.setEnabled(false);
    btnExport.addActionListener(__ -> onExportCallbacks.forEach(Runnable::run));

    GroupLayout bottomLayout = new GroupLayout(this);
    bottomLayout.setHorizontalGroup(bottomLayout.createSequentialGroup()
        .addGroup(bottomLayout.createSequentialGroup()
            .addComponent(btnSelectAll)
            .addComponent(btnClearAll)
            .addPreferredGap(UNRELATED)
            .addComponent(btnExport)
        ));
    bottomLayout.setVerticalGroup(bottomLayout.createParallelGroup(BASELINE)
        .addComponent(btnExport)
        .addComponent(btnSelectAll)
        .addComponent(btnClearAll)
    );
    setLayout(bottomLayout);
  }

  public void onSelectAll(Runnable runnable) {
    onSelectAllCallbacks.add(runnable);
  }

  public void onClearAll(Runnable runnable) {
    onClearAllCallbacks.add(runnable);
  }

  public void onExport(Runnable runnable) {
    onExportCallbacks.add(runnable);
  }

  public void enableExport() {
    btnExport.setEnabled(true);
  }

  public void disableExport() {
    btnExport.setEnabled(false);
  }


  public void toggleClearAll() {
    btnClearAll.setVisible(true);
    btnSelectAll.setVisible(false);
  }

  public void toggleSelectAll() {
    btnClearAll.setVisible(false);
    btnSelectAll.setVisible(true);
  }
}
