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

package org.opendatakit.briefcase.delivery.ui.transfer;

import static org.opendatakit.briefcase.reused.model.Operation.PULL;
import static org.opendatakit.briefcase.reused.model.Operation.PUSH;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTarget;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTargetPanel;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTargetPanelForm;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source.PullSource;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target.PushTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.Operation;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer;

@SuppressWarnings("checkstyle:MethodName")
public class TransferPanelForm<T extends SourceOrTarget> {
  public JPanel container;
  private SourceOrTargetPanel<T> sourceOrTargetPanel;
  private SourceOrTargetPanelForm sourceOrTargetPanelForm;
  private JPanel top;
  private JPanel actions;
  private final TransferFormsTable formsTable;
  private final TransferFormsTableView formsTableView;
  private JScrollPane scrollPane;
  private JButton selectAllButton;
  private JButton clearAllButton;
  private JButton actionButton;
  private JButton cancelButton;
  private JPanel leftActions;
  private JPanel rightActions;
  private JProgressBar progressBar;
  private boolean working = false;
  private boolean alreadyShownOnce = false;
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();

  private TransferPanelForm(SourceOrTargetPanel<T> sourceOrTargetPanel, TransferFormsTable formsTable, Operation operation) {
    this.sourceOrTargetPanel = sourceOrTargetPanel;
    this.sourceOrTargetPanelForm = sourceOrTargetPanel.getContainer();
    this.formsTable = formsTable;
    this.formsTableView = formsTable.getView();
    $$$setupUI$$$();
    actionButton.setText(operation.getName());

    formsTable.onChange(this::triggerOnChange);
    selectAllButton.addActionListener(__ -> formsTable.selectAll());
    clearAllButton.addActionListener(__ -> formsTable.clearAll());
  }

  public static TransferPanelForm<PullSource> pull(Http http, Path briefcaseDir, TransferForms forms) {
    return new TransferPanelForm<>(SourceOrTargetPanel.pull(http, briefcaseDir), TransferFormsTable.from(forms, PULL), PULL);
  }

  public static TransferPanelForm<PushTarget> push(Http http, SubmissionMetadataPort submissionMetadataPort, TransferForms forms) {
    return new TransferPanelForm<>(SourceOrTargetPanel.push(http, submissionMetadataPort), TransferFormsTable.from(forms, PUSH), PUSH);
  }

  public void onSelect(Consumer<T> callback) {
    sourceOrTargetPanel.onSelect(callback);
  }

  public void onReset(Runnable callback) {
    sourceOrTargetPanel.onReset(callback);
  }

  private void triggerOnChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  public void onChange(Runnable runnable) {
    onChangeCallbacks.add(runnable);
  }

  public void onAction(Runnable callback) {
    actionButton.addActionListener(__ -> callback.run());
  }

  public void onCancel(Runnable callback) {
    cancelButton.addActionListener(__ -> callback.run());
  }

  public void refresh() {
    formsTable.refresh();
  }

  public void showClearAll() {
    selectAllButton.setVisible(false);
    clearAllButton.setVisible(true);
  }

  public void showSelectAll() {
    selectAllButton.setVisible(true);
    clearAllButton.setVisible(false);
  }

  public void enableSelectAll() {
    selectAllButton.setEnabled(!working);
  }

  public void disableSelectAll() {
    selectAllButton.setEnabled(false);
  }

  public void enableAction() {
    actionButton.setEnabled(!working);
  }

  public void disableAction() {
    actionButton.setEnabled(false);
  }

  public void setWorking() {
    working = true;
    progressBar.setVisible(true);
    sourceOrTargetPanel.disableInteraction();
    formsTable.disableInteraction();
    selectAllButton.setEnabled(false);
    clearAllButton.setEnabled(false);
    cancelButton.setVisible(true);
    cancelButton.setEnabled(true);
    actionButton.setVisible(false);
  }

  public void unsetWorking() {
    working = false;
    progressBar.setVisible(false);
    sourceOrTargetPanel.enableInteraction();
    formsTable.enableInteraction();
    selectAllButton.setEnabled(true);
    clearAllButton.setEnabled(true);
    cancelButton.setVisible(false);
    actionButton.setVisible(true);
  }

  public Optional<T> preloadOption(RemoteServer server) {
    return sourceOrTargetPanel.preloadOption(server);
  }

  public void onReady(Runnable callback) {
    this.container.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        if (!alreadyShownOnce) {
          callback.run();
          alreadyShownOnce = true;
        }
      }
    });
  }

  private void createUIComponents() {

  }

  public void clearAllStatusLines() {
    formsTableView.clearAllStatusLines();
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
    top = new JPanel();
    top.setLayout(new CardLayout(0, 0));
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(top, gbc);
    top.add(sourceOrTargetPanelForm.$$$getRootComponent$$$(), "Card1");
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer4, gbc);
    scrollPane = new JScrollPane();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(scrollPane, gbc);
    scrollPane.setViewportView(formsTableView);
    actions = new JPanel();
    actions.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
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
    selectAllButton.setEnabled(false);
    selectAllButton.setText("Select All");
    leftActions.add(selectAllButton);
    clearAllButton = new JButton();
    clearAllButton.setText("Clear All");
    clearAllButton.setVisible(false);
    leftActions.add(clearAllButton);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    actions.add(spacer5, gbc);
    rightActions = new JPanel();
    rightActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(rightActions, gbc);
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    rightActions.add(progressBar);
    actionButton = new JButton();
    actionButton.setEnabled(false);
    actionButton.setText("Action");
    rightActions.add(actionButton);
    cancelButton = new JButton();
    cancelButton.setText("Cancel");
    cancelButton.setVisible(false);
    rightActions.add(cancelButton);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.gridheight = 7;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer6, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 7;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer7, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }


}
