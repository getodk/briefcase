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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.ui.export.components.ConfigurationDialog;
import org.opendatakit.briefcase.ui.export.components.ExportFormsTable;
import org.opendatakit.briefcase.ui.export.components.ExportFormsTableView;

@SuppressWarnings("checkstyle:MethodName")
public class ExportPanelForm {
  private final ExportFormsTable formsTable;
  private final ExportFormsTableView formsTableForm;
  private JPanel container;
  private JPanel actions;
  private JPanel leftActions;
  private JPanel rightActions;
  private JButton selectAllButton;
  private JButton clearAllButton;
  JButton exportButton;
  private JProgressBar exportProgressBar;
  private JPanel defaultConfPanel;
  private JButton setDefaultConfButton;
  private Optional<ExportConfiguration> defaultConf = Optional.empty();
  private List<Consumer<ExportConfiguration>> onDefaultConfSetCallbacks = new ArrayList<>();
  private List<Runnable> onDefaultConfResetCallbacks = new ArrayList<>();

  private ExportPanelForm(ExportFormsTable formsTable, BriefcasePreferences appPreferences, ExportConfiguration initialConf) {
    this.formsTable = formsTable;
    this.formsTableForm = formsTable.getView();
    $$$setupUI$$$();

    if (!initialConf.isEmpty())
      setDefaultConf(initialConf);
    else
      resetDefaultConf();

    selectAllButton.addActionListener(__ -> formsTable.selectAll());
    clearAllButton.addActionListener(__ -> formsTable.clearAll());
    setDefaultConfButton.addActionListener(ignored -> {
      ConfigurationDialog dialog = ConfigurationDialog.defaultPanel(defaultConf, appPreferences.getRememberPasswords().orElse(false));
      dialog.onOK(this::setDefaultConf);
      dialog.onRemove(this::resetDefaultConf);
      dialog.open();
    });
  }

  private void setDefaultConf(ExportConfiguration conf) {
    defaultConf = Optional.of(conf);
    onDefaultConfSetCallbacks.forEach(callback -> callback.accept(conf));
    setDefaultConfButton.setText("Edit Default Configuration");
  }

  private void resetDefaultConf() {
    defaultConf = Optional.empty();
    onDefaultConfResetCallbacks.forEach(Runnable::run);
    setDefaultConfButton.setText("Set Default Configuration");
  }

  public static ExportPanelForm from(ExportForms forms, BriefcasePreferences appPreferences, ExportConfiguration defaultConf) {
    return new ExportPanelForm(
        ExportFormsTable.from(forms),
        appPreferences,
        defaultConf
    );
  }

  public JPanel getContainer() {
    return container;
  }

  public ExportFormsTable getFormsTable() {
    return formsTable;
  }

  void onExport(Runnable callback) {
    exportButton.addActionListener(__ -> callback.run());
  }

  void onChange(Runnable callback) {
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

  void setExporting() {
    exportProgressBar.setVisible(true);
    formsTable.setEnabled(false);
    selectAllButton.setEnabled(false);
    clearAllButton.setEnabled(false);
    exportButton.setEnabled(false);
    setDefaultConfButton.setEnabled(false);
  }

  void unsetExporting() {
    exportProgressBar.setVisible(false);
    formsTable.setEnabled(true);
    selectAllButton.setEnabled(true);
    clearAllButton.setEnabled(true);
    exportButton.setEnabled(true);
    setDefaultConfButton.setEnabled(true);
  }

  public void refresh() {
    formsTable.refresh();
  }

  public void onDefaultConfSet(Consumer<ExportConfiguration> callback) {
    onDefaultConfSetCallbacks.add(callback);
  }

  public void onDefaultConfReset(Runnable callback) {
    onDefaultConfResetCallbacks.add(callback);
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
    actions = new JPanel();
    actions.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 9;
    gbc.weightx = 1.0;
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
    rightActions = new JPanel();
    rightActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(rightActions, gbc);
    exportProgressBar = new JProgressBar();
    exportProgressBar.setEnabled(true);
    exportProgressBar.setIndeterminate(true);
    exportProgressBar.setStringPainted(false);
    exportProgressBar.setVisible(false);
    rightActions.add(exportProgressBar);
    exportButton = new JButton();
    exportButton.setEnabled(false);
    exportButton.setName("export");
    exportButton.setText("Export");
    rightActions.add(exportButton);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    actions.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
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
    gbc.gridheight = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer4, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 10;
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
    gbc.gridy = 8;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer7, gbc);
    final JScrollPane scrollPane1 = new JScrollPane();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridheight = 3;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(scrollPane1, gbc);
    formsTableForm.setPreferredScrollableViewportSize(new Dimension(480, 400));
    scrollPane1.setViewportView(formsTableForm);
    defaultConfPanel = new JPanel();
    defaultConfPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(defaultConfPanel, gbc);
    setDefaultConfButton = new JButton();
    setDefaultConfButton.setText("Set Default Configuration");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    defaultConfPanel.add(setDefaultConfButton, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    defaultConfPanel.add(spacer8, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    defaultConfPanel.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer10, gbc);
    final JLabel label1 = new JLabel();
    label1.setHorizontalAlignment(0);
    label1.setText("To override the default configuration for each form, press the gear icon");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.weightx = 1.0;
    container.add(label1, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
