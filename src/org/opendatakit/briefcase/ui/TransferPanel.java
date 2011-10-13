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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.EndPointType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;

public class TransferPanel extends JPanel {

  /**
	 * 
	 */
  private static final long serialVersionUID = -2192404551259501394L;

  private JComboBox listOriginDataSource;
  private JButton btnOriginAction;
  private JLabel lblOrigin;
  private JTextField txtOriginName;
  private ServerConnectionInfo originServerInfo = null;

  private JComboBox listDestinationDataSink;
  private JButton btnDestinationAction;
  private JLabel lblDestination;
  private JTextField txtDestinationName;
  private ServerConnectionInfo destinationServerInfo = null;

  private FormTransferTable formTransferTable;
  private JButton btnSelectOrClearAllForms;
  private JButton btnTransfer;

  private TerminationFuture terminationFuture;
  
  /**
   * UI changes related to the selection of the origin location from drop-down
   * box.
   */
  class OriginSourceListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      JComboBox cb = (JComboBox) e.getSource();
      String strSelection = (String) cb.getSelectedItem();
      EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection)
          : null;

      if (selection != null) {
        if (EndPointType.AGGREGATE_0_9_X_CHOICE.equals(selection)
            || EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
          lblOrigin.setText("URL:");
          txtOriginName.setText("");
          txtOriginName.setEditable(false);
          txtOriginName.setVisible(true);
          btnOriginAction.setText("Connect...");
          btnOriginAction.setVisible(true);
        } else if (EndPointType.BRIEFCASE_CHOICE.equals(selection)) {
          lblOrigin.setText("Local Briefcase");
          txtOriginName.setText("");
          txtOriginName.setEditable(false);
          txtOriginName.setVisible(false);
          TransferPanel.this.updateFormStatuses();
          btnOriginAction.setText("Choose...");
          btnOriginAction.setVisible(false);
        } else {
          lblOrigin.setText("Directory:");
          txtOriginName.setText("");
          txtOriginName.setEditable(true);
          txtOriginName.setVisible(true);
          btnOriginAction.setText("Choose...");
          btnOriginAction.setVisible(true);
        }
      }
    }
  }

  /**
   * UI changes related to the selection of the destination location from
   * drop-down box.
   */
  class DestinationSinkListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      JComboBox cb = (JComboBox) e.getSource();
      String strSelection = (String) cb.getSelectedItem();
      EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection)
          : null;

      if (selection != null) {
        if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
          lblDestination.setText("URL:");
          txtDestinationName.setText("");
          txtDestinationName.setEditable(false);
          txtDestinationName.setVisible(true);
          btnDestinationAction.setText("Connect...");
          btnDestinationAction.setVisible(true);
        } else if (EndPointType.BRIEFCASE_CHOICE.equals(selection)) {
          lblDestination.setText("Local Briefcase");
          txtDestinationName.setText("");
          txtDestinationName.setEditable(false);
          txtDestinationName.setVisible(false);
          btnDestinationAction.setText("Choose...");
          btnDestinationAction.setVisible(false);
        } else {
          throw new IllegalStateException("unexpected case");
        }
      }
    }
  }

  /**
   * Handle click-action for origin "Choose..." and "Connect..." button and the
   * related UI updates (e.g., populating the available forms list).
   */
  class OriginActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      String strSelection = (String) listOriginDataSource.getSelectedItem();
      EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection)
          : null;

      if (EndPointType.AGGREGATE_0_9_X_CHOICE.equals(selection)) {
        // need to show (modal) connect dialog...
        LegacyServerConnectionDialog d = new LegacyServerConnectionDialog(originServerInfo, false);
        d.setVisible(true);
        if (d.isSuccessful()) {
          originServerInfo = d.getServerInfo();
          txtOriginName.setText(originServerInfo.getUrl());
          TransferPanel.this.updateFormStatuses();
        }
      } else if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // need to show (modal) connect dialog...
        ServerConnectionDialog d = new ServerConnectionDialog(originServerInfo, false);
        d.setVisible(true);
        if (d.isSuccessful()) {
          originServerInfo = d.getServerInfo();
          txtOriginName.setText(originServerInfo.getUrl());
          TransferPanel.this.updateFormStatuses();
        }
      } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
        // odkCollect...
        ODKCollectFileChooser fc = new ODKCollectFileChooser(TransferPanel.this);
        int retVal = fc.showOpenDialog(TransferPanel.this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
          txtOriginName.setText(fc.getSelectedFile().getAbsolutePath());
          TransferPanel.this.updateFormStatuses();
        }
      } else {
        throw new IllegalStateException("unexpected case");
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
      String strSelection = (String) listDestinationDataSink.getSelectedItem();
      EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection)
          : null;

      if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // need to show (modal) connect dialog...
        ServerConnectionDialog d = new ServerConnectionDialog(destinationServerInfo, true);
        d.setVisible(true);
        if (d.isSuccessful()) {
          ServerConnectionInfo info = d.getServerInfo();
          if (info.isOpenRosaServer()) {
            destinationServerInfo = d.getServerInfo();
            txtDestinationName.setText(destinationServerInfo.getUrl());
          } else {
            JOptionPane.showMessageDialog(TransferPanel.this,
                "Server is not an ODK Aggregate 1.0 server", "Invalid Server URL",
                JOptionPane.ERROR_MESSAGE);
          }
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

      String strSelection;
      strSelection = (String) listOriginDataSource.getSelectedItem();
      EndPointType originSelection = (strSelection != null) ? EndPointType.fromString(strSelection)
          : null;
      strSelection = (String) listDestinationDataSink.getSelectedItem();
      EndPointType destinationSelection = (strSelection != null) ? EndPointType
          .fromString(strSelection) : null;

      List<FormStatus> formsToTransfer = formTransferTable.getSelectedForms();
      // clear the transfer history...
      for (FormStatus fs : formsToTransfer) {
        fs.clearStatusHistory();
      }

      try {
        btnTransfer.setEnabled(false);
        terminationFuture.reset();
        if (EndPointType.AGGREGATE_0_9_X_CHOICE.equals(originSelection)
            || EndPointType.AGGREGATE_1_0_CHOICE.equals(originSelection)) {
          if (EndPointType.AGGREGATE_1_0_CHOICE.equals(destinationSelection)) {
            TransferAction.transferServerViaToServer(originServerInfo, destinationServerInfo,
                terminationFuture, formsToTransfer);
          } else if (EndPointType.BRIEFCASE_CHOICE.equals(destinationSelection)) {
            TransferAction.transferServerViaToBriefcase(originServerInfo, 
                terminationFuture, formsToTransfer);
          } else {
            throw new IllegalStateException("unhandled case");
          }

        } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(originSelection)) {

          if (EndPointType.AGGREGATE_1_0_CHOICE.equals(destinationSelection)) {
            TransferAction.transferODKViaToServer(new File(txtOriginName.getText()),
                destinationServerInfo, terminationFuture, formsToTransfer);
          } else if (EndPointType.BRIEFCASE_CHOICE.equals(destinationSelection)) {
            TransferAction.transferODKViaToBriefcase(terminationFuture, new File(txtOriginName.getText()),
                formsToTransfer);
          } else {
            throw new IllegalStateException("unhandled case");
          }
        } else if (EndPointType.BRIEFCASE_CHOICE.equals(originSelection)) {

          if (EndPointType.AGGREGATE_1_0_CHOICE.equals(destinationSelection)) {
            TransferAction.transferBriefcaseViaToServer(destinationServerInfo, terminationFuture, formsToTransfer);
          } else {
            throw new IllegalStateException("unhandled case");
          }
        } else {
          throw new IllegalStateException("unhandled case");
        }
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(TransferPanel.this,
            "Briefcase action failed: " + ex.getMessage(), "Briefcase Action Failed",
            JOptionPane.ERROR_MESSAGE);
      } finally {
        btnTransfer.setEnabled(true);
      }
    }
  }

  /**
   * Create the transfer-from-to panel.
   * 
   * @param txtBriefcaseDir
   */
  public TransferPanel(TerminationFuture terminationFuture) {
    super();
    this.terminationFuture = terminationFuture;
    JLabel lblGetDataFrom = new JLabel("Get data from:");

    listOriginDataSource = new JComboBox(new String[] {
        EndPointType.AGGREGATE_0_9_X_CHOICE.toString(),
        EndPointType.AGGREGATE_1_0_CHOICE.toString(), EndPointType.BRIEFCASE_CHOICE.toString(),
        EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.toString() });
    listOriginDataSource.addActionListener(new OriginSourceListener());

    lblOrigin = new JLabel("Origin:");

    txtOriginName = new JTextField();
    txtOriginName.setColumns(10);
    txtOriginName.addFocusListener(new FocusListener() {

      @Override
      public void focusGained(FocusEvent e) {
        // don't care...
      }

      @Override
      public void focusLost(FocusEvent e) {
        TransferPanel.this.updateFormStatuses();
      }
    });

    btnOriginAction = new JButton("Choose...");
    btnOriginAction.addActionListener(new OriginActionListener());

    JSeparator separatorSourceDestination = new JSeparator();

    JLabel lblSendDataTo = new JLabel("Send data to:");

    listDestinationDataSink = new JComboBox(new String[] {
        EndPointType.AGGREGATE_1_0_CHOICE.toString(), EndPointType.BRIEFCASE_CHOICE.toString() });

    listDestinationDataSink.addActionListener(new DestinationSinkListener());

    lblDestination = new JLabel("Destination:");

    txtDestinationName = new JTextField();
    txtDestinationName.setColumns(10);

    btnDestinationAction = new JButton("Choose...");
    btnDestinationAction.addActionListener(new DestinationActionListener());

    JLabel lblFormsToTransfer = new JLabel("Forms to Transfer:");

    btnSelectOrClearAllForms = new JButton("Select all");

    btnTransfer = new JButton("Transfer");

    formTransferTable = new FormTransferTable(btnSelectOrClearAllForms, btnTransfer);

    JScrollPane scrollPane = new JScrollPane(formTransferTable);

    JSeparator separatorFormsList = new JSeparator();

    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
        groupLayout
            .createSequentialGroup()
            .addContainerGap()
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.LEADING)
                    .addGroup(
                        Alignment.TRAILING,
                        groupLayout
                            .createSequentialGroup()
                            .addGroup(
                                groupLayout.createParallelGroup(Alignment.TRAILING)
                                    .addComponent(lblOrigin).addComponent(lblGetDataFrom)
                                    .addComponent(lblSendDataTo).addComponent(lblDestination))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(
                                groupLayout
                                    .createParallelGroup(Alignment.LEADING)
                                    .addGroup(
                                        groupLayout
                                            .createSequentialGroup()
                                            .addComponent(txtOriginName,
                                                GroupLayout.PREFERRED_SIZE, 335, Short.MAX_VALUE)
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(btnOriginAction))
                                    .addComponent(listOriginDataSource, GroupLayout.PREFERRED_SIZE,
                                        443, Short.MAX_VALUE)
                                    .addGroup(
                                        groupLayout
                                            .createSequentialGroup()
                                            .addComponent(txtDestinationName,
                                                GroupLayout.PREFERRED_SIZE, 335, Short.MAX_VALUE)
                                            .addPreferredGap(ComponentPlacement.RELATED)
                                            .addComponent(btnDestinationAction))
                                    .addComponent(listDestinationDataSink,
                                        GroupLayout.PREFERRED_SIZE, 443, Short.MAX_VALUE)))
                    .addComponent(separatorSourceDestination, Alignment.TRAILING,
                        GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
                    .addComponent(lblFormsToTransfer, Alignment.LEADING)
                    .addGroup(
                        Alignment.TRAILING,
                        groupLayout.createSequentialGroup().addComponent(btnSelectOrClearAllForms)
                            .addPreferredGap(ComponentPlacement.RELATED, 367, Short.MAX_VALUE)
                            .addComponent(btnTransfer))
                    .addComponent(scrollPane, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 553,
                        Short.MAX_VALUE)
                    .addComponent(separatorFormsList, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE,
                        553, Short.MAX_VALUE)).addContainerGap()));
    groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
        groupLayout
            .createSequentialGroup()
            .addContainerGap()
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblGetDataFrom)
                    .addComponent(listOriginDataSource, GroupLayout.PREFERRED_SIZE, 25,
                        GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblOrigin)
                    .addComponent(txtOriginName, GroupLayout.PREFERRED_SIZE,
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnOriginAction))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(separatorSourceDestination, GroupLayout.PREFERRED_SIZE, 2,
                GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblSendDataTo)
                    .addComponent(listDestinationDataSink, GroupLayout.PREFERRED_SIZE, 29,
                        GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDestination)
                    .addComponent(txtDestinationName, GroupLayout.PREFERRED_SIZE,
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDestinationAction))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(separatorFormsList, GroupLayout.PREFERRED_SIZE, 2,
                GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(lblFormsToTransfer)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 100, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(btnSelectOrClearAllForms).addComponent(btnTransfer))
            .addContainerGap()));
    setLayout(groupLayout);

    // and finally, set the initial selections in the combo boxes...
    listOriginDataSource.setSelectedIndex(1);
    listDestinationDataSink.setSelectedIndex(1);

    // set up the transfer action...
    btnTransfer.addActionListener(new TransferActionListener());
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    Component[] com = this.getComponents();
    for (int a = 0; a < com.length; a++) {
      com[a].setEnabled(enabled);
    }
  }

  public void updateFormStatuses() {
    List<FormStatus> statuses = new ArrayList<FormStatus>();

    // determine what our origin is...
    String strSelection = (String) listOriginDataSource.getSelectedItem();
    EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection) : null;
    if (selection != null) {
      if (EndPointType.AGGREGATE_0_9_X_CHOICE.equals(selection)
          || EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // clear the list of forms first...
        formTransferTable.setFormStatusList(statuses);
        terminationFuture.reset();
        TransferAction.retrieveAvailableFormsFromServer(originServerInfo, terminationFuture);
        // list will be communicated back via the
        // RetrieveAvailableFormsSucceededEvent
      } else if (EndPointType.BRIEFCASE_CHOICE.equals(selection)) {
        List<LocalFormDefinition> forms = FileSystemUtils.getBriefcaseFormList(
            BriefcasePreferences.getBriefcaseDirectoryProperty());
        for (LocalFormDefinition f : forms) {
          statuses.add(new FormStatus(f));
        }
        formTransferTable.setFormStatusList(statuses);
      } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
        List<LocalFormDefinition> forms = FileSystemUtils.getODKFormList(txtOriginName.getText());
        for (LocalFormDefinition f : forms) {
          statuses.add(new FormStatus(f));
        }
        formTransferTable.setFormStatusList(statuses);
      } else {
        throw new IllegalStateException("unexpected case");
      }
    }
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void formsAvailableFromServer(RetrieveAvailableFormsFailedEvent event) {
    JOptionPane.showMessageDialog(TransferPanel.this, "Accessing the server failed with error: "
        + event.getReason(), "Accessing Server Failed", JOptionPane.ERROR_MESSAGE);
  }

}
