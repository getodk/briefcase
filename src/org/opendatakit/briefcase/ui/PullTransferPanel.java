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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.EndPointType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;
import org.opendatakit.briefcase.util.WebUtils;

/**
 * Pull forms and data to external locations.
 *
 * @author mitchellsundt@gmail.com
 */
public class PullTransferPanel extends JPanel {

  /**
   *
   */
  private static final long serialVersionUID = -2192404551259501394L;

  public static final String TAB_NAME = "Pull";

  private static final String DOWNLOADING_DOT_ETC = "Downloading..........";
  static final BriefcasePreferences PREFERENCES =
      BriefcasePreferences.forClass(PullTransferPanel.class);

  private JComboBox<String> listOriginDataSource;
  private JButton btnOriginAction;
  private JLabel lblOrigin;
  private JTextField txtOriginName;
  private ServerConnectionInfo originServerInfo = null;

  private FormTransferTable formTransferTable;
  private JButton btnSelectOrClearAllForms;
  private JLabel lblDownloading;
  private JButton btnTransfer;
  private JButton btnCancel;

  private boolean transferStateActive = false;
  private TerminationFuture terminationFuture;

  /**
   * UI changes related to the selection of the origin location from drop-down
   * box.
   */
  class OriginSourceListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      EndPointType selection = getSelectedEndPointType();
      if (selection != null) {
        if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
          lblOrigin.setText("URL:");
          txtOriginName.setText("");
          txtOriginName.setEditable(false);
          txtOriginName.setVisible(true);
          btnOriginAction.setText("Connect...");
          btnOriginAction.setVisible(true);
        } else if (EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.equals(selection)) {
          lblOrigin.setText("ODK Directory:");
          txtOriginName.setText("");
          txtOriginName.setEditable(true);
          txtOriginName.setVisible(true);
          btnOriginAction.setText("Choose...");
          btnOriginAction.setVisible(true);
        } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
          lblOrigin.setText("SD Card:");
          txtOriginName.setText("");
          txtOriginName.setEditable(true);
          txtOriginName.setVisible(true);
          btnOriginAction.setText("Choose...");
          btnOriginAction.setVisible(true);
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
      EndPointType selection = getSelectedEndPointType();
      originServerInfo = initServerInfoWithPreferences(selection);
      if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // need to show (modal) connect dialog...
        ServerConnectionDialog d = new ServerConnectionDialog(
            (Window) PullTransferPanel.this.getTopLevelAncestor(), originServerInfo, false);
        d.setVisible(true);
        if (d.isSuccessful()) {
          // We reset the Http context to force next request to authenticate itself
          WebUtils.resetHttpContext();
          originServerInfo = d.getServerInfo();
          txtOriginName.setText(originServerInfo.getUrl());
          PREFERENCES.put(BriefcasePreferences.AGGREGATE_1_0_URL, originServerInfo.getUrl());
          PREFERENCES.put(BriefcasePreferences.USERNAME, originServerInfo.getUsername());
          if (BriefcasePreferences.getStorePasswordsConsentProperty())
            PREFERENCES.put(BriefcasePreferences.PASSWORD, new String(originServerInfo.getPassword()));
          PullTransferPanel.this.updateFormStatuses();
        } else {
          if (!BriefcasePreferences.getStorePasswordsConsentProperty()) {
            // We need to clear the forms table because we have lost any password
            // by opening the connection dialog
            formTransferTable.setFormStatusList(new ArrayList<>());
          }
        }
      } else if (EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.equals(selection)) {
        // odkCollect...
        WrappedFileChooser fc = new WrappedFileChooser(PullTransferPanel.this,
            new ODKCollectFileChooser(PullTransferPanel.this));
        String filePath = txtOriginName.getText();
        if (filePath != null && filePath.trim().length() != 0) {
          fc.setSelectedFile(new File(filePath.trim()));
        }
        int retVal = fc.showDialog();
        if (retVal == JFileChooser.APPROVE_OPTION) {
          txtOriginName.setText(fc.getSelectedFile().getAbsolutePath());
          PullTransferPanel.this.updateFormStatuses();
        }
      } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
        // odkCollect...
        int retVal;
        MountedSDCardChooserDialog fc = new MountedSDCardChooserDialog(
            (JFrame) SwingUtilities.getRoot(PullTransferPanel.this), txtOriginName.getText());
        retVal = fc.showDialog();
        if (retVal == JFileChooser.APPROVE_OPTION) {
          txtOriginName.setText(fc.getSelectedFile().getAbsolutePath());
          PullTransferPanel.this.updateFormStatuses();
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
      EndPointType originSelection = getSelectedEndPointType();
      List<FormStatus> formsToTransfer = formTransferTable.getSelectedForms();
      // clear the transfer history...
      for (FormStatus fs : formsToTransfer) {
        fs.clearStatusHistory();
      }

      try {
        setActiveTransferState(true);
        if (EndPointType.AGGREGATE_1_0_CHOICE.equals(originSelection)) {
          TransferAction.transferServerToBriefcase(originServerInfo, terminationFuture,
              formsToTransfer);
        } else if (EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.equals(originSelection)) {
          TransferAction.transferODKToBriefcase(new File(txtOriginName.getText()),
              terminationFuture, formsToTransfer);
        } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(originSelection)) {
          TransferAction.transferODKToBriefcase(new File(new File(txtOriginName.getText()), "odk"),
              terminationFuture, formsToTransfer);
        } else {
          throw new IllegalStateException("unhandled case");
        }
      } catch (IOException ex) {
        ODKOptionPane.showErrorDialog(PullTransferPanel.this,
            "Briefcase action failed: " + ex.getMessage(), "Briefcase Action Failed");
        setActiveTransferState(false);
      }
    }
  }

  public PullTransferPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP
    this.terminationFuture = terminationFuture;
    JLabel lblGetDataFrom = new JLabel(TAB_NAME + " data from:");

    listOriginDataSource = new JComboBox<>(new String[] {
            EndPointType.AGGREGATE_1_0_CHOICE.toString(),
            EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.toString(),
            EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.toString() });
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
        if (txtOriginName.isEditable()) {
          PullTransferPanel.this.updateFormStatuses();
        }
      }
    });

    btnOriginAction = new JButton("Choose...");
    btnOriginAction.addActionListener(new OriginActionListener());

    btnSelectOrClearAllForms = new JButton("Select all");

    lblDownloading = new JLabel(DOWNLOADING_DOT_ETC);
    lblDownloading.setForeground(lblDownloading.getBackground());
    btnTransfer = new JButton(TAB_NAME);
    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        PullTransferPanel.this.terminationFuture.markAsCancelled(
            new TransferAbortEvent(TAB_NAME + " cancelled by user."));
      }
    });

    formTransferTable = new FormTransferTable(
            btnSelectOrClearAllForms, FormStatus.TransferType.GATHER, btnTransfer, btnCancel);
    formTransferTable.setSourceSelected(true);
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
                                .addComponent(lblOrigin).addComponent(lblGetDataFrom))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                            groupLayout
                                .createParallelGroup(Alignment.LEADING)
                                .addComponent(listOriginDataSource)
                                .addGroup(
                                    Alignment.TRAILING,
                                    groupLayout.createSequentialGroup().addComponent(txtOriginName)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btnOriginAction))))
                // scroll pane
                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                    Short.MAX_VALUE)
                .addGroup(
                    groupLayout.createSequentialGroup().addComponent(btnSelectOrClearAllForms)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(lblDownloading))
                .addGroup(
                    Alignment.TRAILING,
                    groupLayout.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnTransfer).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnCancel))).addContainerGap());
    groupLayout.setVerticalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblGetDataFrom)
                .addComponent(listOriginDataSource))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblOrigin)
                .addComponent(txtOriginName).addComponent(btnOriginAction))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(scrollPane, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(btnSelectOrClearAllForms).addComponent(lblDownloading)
                .addComponent(btnTransfer).addComponent(btnCancel)).addContainerGap());
    setLayout(groupLayout);

    // and finally, set the initial selections in the combo boxes...
    listOriginDataSource.setSelectedIndex(0);

    // set up the transfer action...
    btnTransfer.addActionListener(new TransferActionListener());

    setActiveTransferState(transferStateActive);
    lblDownloading.setText("                     ");
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

    // determine what our origin is...
    String strSelection = (String) listOriginDataSource.getSelectedItem();
    EndPointType selection = (strSelection != null) ? EndPointType.fromString(strSelection) : null;
    if (selection != null) {
      if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        // clear the list of forms first...
        formTransferTable.setFormStatusList(statuses);
        terminationFuture.reset();
        TransferAction.retrieveAvailableFormsFromServer((Window) getTopLevelAncestor(), originServerInfo, terminationFuture);
        // list will be communicated back via the
        // RetrieveAvailableFormsSucceededEvent
      } else if (EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.equals(selection)) {
        File odk = new File(txtOriginName.getText());
        List<OdkCollectFormDefinition> forms = FileSystemUtils.getODKFormList(odk);
        for (OdkCollectFormDefinition f : forms) {
          statuses.add(new FormStatus(FormStatus.TransferType.GATHER, f));
        }
        formTransferTable.setFormStatusList(statuses);
      } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
        File sdcard = new File(txtOriginName.getText());
        File odk = new File(sdcard, "odk");
        List<OdkCollectFormDefinition> forms = FileSystemUtils.getODKFormList(odk);
        for (OdkCollectFormDefinition f : forms) {
          statuses.add(new FormStatus(FormStatus.TransferType.GATHER, f));
        }
        formTransferTable.setFormStatusList(statuses);
      } else {
        throw new IllegalStateException("unexpected case");
      }
    }
  }

  private void updateDownloadingLabel() {
    String text = lblDownloading.getText();
    if (text.equals(DOWNLOADING_DOT_ETC)) {
      text = "Downloading.";
    } else {
      text += ".";
    }
    lblDownloading.setText(text);
  }

  private void setTxtOriginEnabled(boolean active) {
    EndPointType selection = getSelectedEndPointType();

    if (selection != null) {
      if (EndPointType.AGGREGATE_1_0_CHOICE.equals(selection)) {
        txtOriginName.setEditable(false);
      } else if (EndPointType.CUSTOM_ODK_COLLECT_DIRECTORY.equals(selection)) {
        txtOriginName.setEditable(active);
      } else if (EndPointType.MOUNTED_ODK_COLLECT_DEVICE_CHOICE.equals(selection)) {
        txtOriginName.setEditable(active);
      } else {
        throw new IllegalStateException("unexpected case");
      }
    }
  }

  private void setActiveTransferState(boolean active) {
    setTxtOriginEnabled(!active);
    if (active) {
      // don't allow normal actions when we are transferring...
      listOriginDataSource.setEnabled(false);
      btnOriginAction.setEnabled(false);
      btnSelectOrClearAllForms.setEnabled(false);
      btnTransfer.setEnabled(false);
      // enable cancel button
      btnCancel.setEnabled(true);
      // show downloading progress text
      lblDownloading.setText(DOWNLOADING_DOT_ETC);
      lblDownloading.setForeground(lblOrigin.getForeground());
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
    } else {
      // restore normal actions when we aren't transferring...
      listOriginDataSource.setEnabled(true);
      btnOriginAction.setEnabled(true);
      btnSelectOrClearAllForms.setEnabled(true);
      btnTransfer.setEnabled(!formTransferTable.getSelectedForms().isEmpty());
      // disable cancel button
      btnCancel.setEnabled(false);
      // hide downloading progress text (by setting foreground color to
      // background)
      lblDownloading.setText(DOWNLOADING_DOT_ETC);
      lblDownloading.setForeground(lblDownloading.getBackground());
    }
    // remember state...
    transferStateActive = active;
  }

  private EndPointType getSelectedEndPointType() {
    String strSelection = (String) listOriginDataSource.getSelectedItem();
    return EndPointType.fromString(strSelection);
  }

  private ServerConnectionInfo initServerInfoWithPreferences(EndPointType type) {
    ServerConnectionInfo connectionInfo = null;
    if (type == EndPointType.AGGREGATE_1_0_CHOICE) {
      String url = PREFERENCES.get(BriefcasePreferences.AGGREGATE_1_0_URL, "");
      String username = PREFERENCES.get(BriefcasePreferences.USERNAME, "");
      char[] password = BriefcasePreferences.getStorePasswordsConsentProperty() ? PREFERENCES.get(BriefcasePreferences.PASSWORD, "").toCharArray() : new char[0];
      connectionInfo = new ServerConnectionInfo(url, username, password);
    } // There are no preferences needed for the other types.

    return connectionInfo;
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    setActiveTransferState(false);
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    setActiveTransferState(false);
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void updateDetailedStatus(FormStatusEvent fse) {
    updateDownloadingLabel();
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void formsAvailableFromServer(RetrieveAvailableFormsFailedEvent event) {
    ODKOptionPane.showErrorDialog(PullTransferPanel.this,
        "Accessing the server failed with error: " + event.getReason(), "Accessing Server Failed");
  }

}
