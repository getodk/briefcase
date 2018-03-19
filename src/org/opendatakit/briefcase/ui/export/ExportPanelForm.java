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
package org.opendatakit.briefcase.ui.export;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanelForm;
import org.opendatakit.briefcase.ui.export.components.FormsTable;
import org.opendatakit.briefcase.ui.export.components.FormsTableView;

@SuppressWarnings("checkstyle:MethodName")
public class ExportPanelForm {
  private static final String EXPORTING_DOT_ETC = "Exporting..........";
  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = new ScheduledThreadPoolExecutor(1);

  private final ConfigurationPanel confPanel;
  private final FormsTable formsTable;
  private final ConfigurationPanelForm confPanelForm;
  private final FormsTableView formsTableForm;
  private JPanel container;
  private JPanel actions;
  private JPanel leftActions;
  private JPanel rightActions;
  private JButton selectAllButton;
  private JButton clearAllButton;
  private JLabel exportingLabel;
  JButton exportButton;
  private boolean exporting;
  private Optional<ScheduledFuture<?>> scheduledHideExportingLabel = Optional.empty();

  private ExportPanelForm(ConfigurationPanel confPanel, FormsTable formsTable) {
    this.confPanel = confPanel;
    this.confPanelForm = confPanel.getForm();
    this.formsTable = formsTable;
    this.formsTableForm = formsTable.getView();
    $$$setupUI$$$();

    selectAllButton.addActionListener(__ -> formsTable.selectAll());
    clearAllButton.addActionListener(__ -> formsTable.clearAll());

    exporting = false;
  }

  public static ExportPanelForm from(ExportForms forms, ConfigurationPanel confPanel) {
    return new ExportPanelForm(
        confPanel,
        FormsTable.from(forms)
    );
  }

  public JPanel getContainer() {
    return container;
  }

  public ConfigurationPanel getConfPanel() {
    return confPanel;
  }

  public FormsTable getFormsTable() {
    return formsTable;
  }

  void onExport(Runnable callback) {
    exportButton.addActionListener(__ -> callback.run());
  }

  void onChange(Runnable callback) {
    confPanel.onChange(callback);
    formsTable.onChange(callback);
  }

  void enableExport() {
    exportButton.setEnabled(true);
  }

  void disableExport() {
    exportButton.setEnabled(false);
  }

  void toggleClearAll() {
    selectAllButton.setVisible(false);
    clearAllButton.setVisible(true);
  }

  void toggleSelectAll() {
    selectAllButton.setVisible(true);
    clearAllButton.setVisible(false);
  }

  public void setEnabled(boolean enabled) {
    if (enabled) {
      enableUI();
    } else {
      disableUI();
    }
    setExporting(exporting);
  }

  private void setExporting(boolean active) {
    if (active)
      disableUI();
    else
      enableUI();
    exporting = active;
  }

  synchronized public void updateExportingLabel() {
    exportingLabel.setText(exportingLabel.getText().equals(EXPORTING_DOT_ETC) ? "Exporting." : exportingLabel.getText() + ".");

    scheduledHideExportingLabel.ifPresent(scheduledFuture -> scheduledFuture.cancel(false));
    scheduledHideExportingLabel = Optional.of(SCHEDULED_EXECUTOR.schedule(this::hideExporting, 5, SECONDS));
  }

  void disableUI() {
    for (Component c : container.getComponents())
      c.setEnabled(false);
    container.setEnabled(false);
  }

  void enableUI() {
    for (Component c : container.getComponents())
      c.setEnabled(true);
    container.setEnabled(true);
  }

  public void refresh() {
    formsTable.refresh();
  }

  public void showExporting() {
    exportingLabel.setText(EXPORTING_DOT_ETC);
    exportingLabel.setVisible(true);
  }

  public void hideExporting() {
    exportingLabel.setVisible(false);
    exportingLabel.setText(EXPORTING_DOT_ETC);
  }

  private void createUIComponents() {
    // Custom creation of components occurs inside the constructor
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    container = new JPanel();
    container.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(confPanelForm.$$$getRootComponent$$$(), gbc);
    actions = new JPanel();
    actions.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 7;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(actions, gbc);
    leftActions = new JPanel();
    leftActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(leftActions, gbc);
    selectAllButton = new JButton();
    selectAllButton.setText("Select All");
    leftActions.add(selectAllButton);
    clearAllButton = new JButton();
    clearAllButton.setText("Clear All");
    leftActions.add(clearAllButton);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    actions.add(spacer1, gbc);
    rightActions = new JPanel();
    rightActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(rightActions, gbc);
    exportingLabel = new JLabel();
    exportingLabel.setText(EXPORTING_DOT_ETC);
    exportingLabel.setVisible(false);
    rightActions.add(exportingLabel);
    exportButton = new JButton();
    exportButton.setEnabled(false);
    exportButton.setName("export");
    exportButton.setText("Export");
    rightActions.add(exportButton);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    actions.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.gridheight = 8;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer4, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 8;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer5, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer7, gbc);
    final JScrollPane scrollPane1 = new JScrollPane();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(scrollPane1, gbc);
    scrollPane1.setViewportView(formsTableForm);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
