/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import org.apache.http.util.TextUtils;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.EndPointType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.model.UpdatedBriefcaseFormDefinitionEvent;
import org.opendatakit.briefcase.push.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;

/**
 * Push forms and data to external locations.
 *
 * @author mitchellsundt@gmail.com
 */
public class PushTransferPanel extends JPanel {

  private static final long serialVersionUID = -2192404551259501394L;

  public static final String TAB_NAME = "Push";

  private final BriefcasePreferences tabPreferences;
  private final Analytics analytics;

  private JComboBox<String> listDestinationDataSink;
  private JButton btnDestinationAction;
  private JLabel lblDestination;
  private JTextField txtDestinationName;
  private ServerConnectionInfo destinationServerInfo = null;

  private FormTransferTable formTransferTable;
  private JButton btnSelectOrClearAllForms;
  private JButton btnTransfer;
  private JButton btnCancel;

  private boolean transferStateActive = false;
  private TerminationFuture terminationFuture;

  private JProgressBar pbUploading;
  private final Http http;
  private RemoteServer server;

  /**
   * UI changes related to the selection of the destination location from
   * drop-down box.
   */
  class DestinationSinkListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      EndPointType selection = getSelectedEndPointType();

      if (selection != null) {
        if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
          lblDestination.setText("URL:");
          txtDestinationName.setText("");
          txtDestinationName.setEditable(false);
          txtDestinationName.setVisible(true);
          btnDestinationAction.setText("Connect...");
          btnDestinationAction.setVisible(true);
        } else {
          throw new IllegalStateException("unexpected case");
        }
      }
    }
  }

  /**
   * Handle click-action for destination "Choose..." and "Connect..." button and
   * the related UI updates (e.g., populating the available forms list).
   */
  class DestinationActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      EndPointType selection = getSelectedEndPointType();
      if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // need to show (modal) connect dialog...
        destinationServerInfo = initServerInfoWithPreferences();
        ServerConnectionDialog d = new ServerConnectionDialog(
            (Window) PushTransferPanel.this.getTopLevelAncestor(), destinationServerInfo, true);
        d.setVisible(true);
        if (d.isSuccessful()) {
          destinationServerInfo = d.getServerInfo();
          server = RemoteServer.authenticated(destinationServerInfo.getUrl(), destinationServerInfo.getUsername(), new String(destinationServerInfo.getPassword()));
          txtDestinationName.setText(destinationServerInfo.getUrl());
          formTransferTable.setSourceSelected(!TextUtils.isEmpty(txtDestinationName.getText()));
          tabPreferences.put(BriefcasePreferences.AGGREGATE_1_0_URL, destinationServerInfo.getUrl());
          tabPreferences.put(BriefcasePreferences.USERNAME, destinationServerInfo.getUsername());
          if (BriefcasePreferences.getStorePasswordsConsentProperty())
            tabPreferences.put(BriefcasePreferences.PASSWORD, new String(destinationServerInfo.getPassword()));
        }
      } else {
        throw new IllegalStateException("unexpected case");
      }
    }

  }

  /**
   * Handle click-action for the "Transfer" button. Extracts the settings from
   * the UI and invokes the relevant TransferAction to actually do the work.
   */
  class TransferActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      EndPointType destinationSelection = getSelectedEndPointType();

      List<FormStatus> formsToTransfer = formTransferTable.getSelectedForms();
      // clear the transfer history...
      for (FormStatus fs : formsToTransfer) {
        fs.clearStatusHistory();
      }

      try {
        setActiveTransferState(true);
        if (EndPointType.AGGREGATE_1_0_CHOICE.equals(destinationSelection)) {
          TransferAction.transferBriefcaseToServer(
              destinationServerInfo, terminationFuture, formsToTransfer, http, server);
        } else {
          throw new IllegalStateException("unhandled case");
        }
      } catch (IOException ex) {
        ODKOptionPane.showErrorDialog(PushTransferPanel.this,
            "Briefcase action failed: " + ex.getMessage(), "Briefcase Action Failed");
        setActiveTransferState(false);
      }
    }
  }

  /**
   * Create the transfer-from-to panel.
   */
  public PushTransferPanel(TerminationFuture terminationFuture, BriefcasePreferences tabPreferences, Analytics analytics, Http http) {
    super();
    this.tabPreferences = tabPreferences;
    AnnotationProcessor.process(this);// if not using AOP
    addComponentListener(analytics.buildComponentListener("Push"));
    this.terminationFuture = terminationFuture;
    this.analytics = analytics;
    this.http = http;

    JLabel lblSendDataTo = new JLabel(TAB_NAME + " data to:");

    listDestinationDataSink = new JComboBox<>(
        new String[]{EndPointType.AGGREGATE_1_0_CHOICE.toString()});

    listDestinationDataSink.addActionListener(new DestinationSinkListener());

    lblDestination = new JLabel("Destination:");

    txtDestinationName = new JTextField();
    txtDestinationName.setColumns(10);

    btnDestinationAction = new JButton("Choose...");
    btnDestinationAction.addActionListener(new DestinationActionListener());

    btnSelectOrClearAllForms = new JButton("Select all");

    pbUploading = new JProgressBar();
    pbUploading.setEnabled(true);
    pbUploading.setVisible(false);
    pbUploading.setStringPainted(false);
    pbUploading.setIndeterminate(true);
    btnTransfer = new JButton(TAB_NAME);
    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(__ -> PushTransferPanel.this.terminationFuture.markAsCancelled(
        new TransferAbortEvent(TAB_NAME + " cancelled by user.")));

    formTransferTable = new FormTransferTable(
        btnSelectOrClearAllForms, FormStatus.TransferType.UPLOAD, btnTransfer, btnCancel);

    formTransferTable.setSourceSelected(false);
    JScrollPane scrollPane = new JScrollPane(formTransferTable);

    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(
            groupLayout
                .createParallelGroup(Alignment.LEADING)
                // get-data and origin rows
                .addGroup(
                    Alignment.LEADING,
                    // sequential -- 2 elements - label - widgets
                    groupLayout
                        .createSequentialGroup()
                        .addGroup(
                            groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(lblDestination).addComponent(lblSendDataTo))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                            groupLayout
                                .createParallelGroup(Alignment.LEADING)
                                .addComponent(listDestinationDataSink)
                                .addGroup(
                                    Alignment.TRAILING,
                                    groupLayout.createSequentialGroup()
                                        .addComponent(txtDestinationName)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btnDestinationAction))))
                // scroll pane
                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                    Short.MAX_VALUE)
                .addGroup(
                    groupLayout.createSequentialGroup().addComponent(btnSelectOrClearAllForms)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(pbUploading, 0, GroupLayout.DEFAULT_SIZE, 150))
                .addGroup(
                    Alignment.TRAILING,
                    groupLayout.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnTransfer).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnCancel))).addContainerGap());
    groupLayout.setVerticalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblSendDataTo)
                .addComponent(listDestinationDataSink))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblDestination)
                .addComponent(txtDestinationName).addComponent(btnDestinationAction))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(scrollPane, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(btnSelectOrClearAllForms).addComponent(pbUploading)
                .addComponent(btnTransfer).addComponent(btnCancel)).addContainerGap());
    setLayout(groupLayout);
    listDestinationDataSink.setSelectedIndex(0);

    // set up the transfer action...
    btnTransfer.addActionListener(new TransferActionListener());
    // and update the list of forms...
    updateFormStatuses();
    setActiveTransferState(transferStateActive);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    for (Component aCom : this.getComponents()) {
      aCom.setEnabled(enabled);
    }
    if (enabled) {
      // and then update the widgets based upon the transfer state
      setActiveTransferState(transferStateActive);
    }
  }

  public void updateFormStatuses() {
    List<FormStatus> statuses = new ArrayList<>();

    List<BriefcaseFormDefinition> forms = FileSystemUtils.getBriefcaseFormList();
    for (BriefcaseFormDefinition f : forms) {
      statuses.add(new FormStatus(FormStatus.TransferType.UPLOAD, f));
    }
    formTransferTable.setFormStatusList(statuses);
  }

  private void setActiveTransferState(boolean active) {
    if (active) {
      // don't allow normal actions when we are transferring...
      listDestinationDataSink.setEnabled(false);
      btnDestinationAction.setEnabled(false);
      btnSelectOrClearAllForms.setEnabled(false);
      btnTransfer.setEnabled(false);
      // enable cancel button
      btnCancel.setEnabled(true);
      pbUploading.setVisible(true);
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
    } else {
      // restore normal actions when we aren't transferring...
      listDestinationDataSink.setEnabled(true);
      btnDestinationAction.setEnabled(true);
      btnSelectOrClearAllForms.setEnabled(true);
      btnTransfer.setEnabled(!formTransferTable.getSelectedForms().isEmpty() && !TextUtils.isEmpty(txtDestinationName.getText()));
      // disable cancel button
      btnCancel.setEnabled(false);
      pbUploading.setVisible(false);
    }
    // remember state...
    transferStateActive = active;
  }

  private EndPointType getSelectedEndPointType() {
    String strSelection = (String) listDestinationDataSink.getSelectedItem();
    return EndPointType.fromString(strSelection);
  }

  private ServerConnectionInfo initServerInfoWithPreferences() {
    String url = tabPreferences.get(BriefcasePreferences.AGGREGATE_1_0_URL, "");
    String username = tabPreferences.get(BriefcasePreferences.USERNAME, "");
    char[] password = BriefcasePreferences.getStorePasswordsConsentProperty()
        ? tabPreferences.get(BriefcasePreferences.PASSWORD, "").toCharArray()
        : new char[0];
    return new ServerConnectionInfo(url, username, password);
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    setActiveTransferState(false);
    analytics.event("Push", "Transfer", "Failure", null);
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    setActiveTransferState(false);
    analytics.event("Push", "Transfer", "Success", null);
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void formsAvailableFromServer(RetrieveAvailableFormsFailedEvent event) {
    ODKOptionPane.showErrorDialog(PushTransferPanel.this,
        "Accessing the server failed with error: " + event.getReason(), "Accessing Server Failed");
  }

  @EventSubscriber(eventClass = UpdatedBriefcaseFormDefinitionEvent.class)
  public void briefcaseFormListChanges(UpdatedBriefcaseFormDefinitionEvent event) {
    updateFormStatuses();
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    tabPreferences.remove(BriefcasePreferences.PASSWORD);
  }
}
