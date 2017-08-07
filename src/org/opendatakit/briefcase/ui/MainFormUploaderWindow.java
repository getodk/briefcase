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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.TransferAction;

public class MainFormUploaderWindow {

  private static final Log log = LogFactory.getLog(MainFormUploaderWindow.class);
  private static final String FORM_UPLOADER_VERSION = "ODK FormUploader - " + BriefcasePreferences.VERSION;
  private static final String UPLOADING_DOT_ETC = "Uploading..........";

  private ServerConnectionInfo destinationServerInfo = null;
  private JFrame frame;
  private JTextField txtFormDefinitionFile;
  private JButton btnChoose;
  private JTextField txtDestinationName;
  private JButton btnChooseServer;

  private JLabel  lblUploading;
  private JButton btnDetails;
  private JButton btnUploadForm;
  private JButton btnCancel;

  private FormStatus fs = null;

  private JButton btnClose;

  private boolean transferStateActive = false;
  private final TerminationFuture terminationFuture = new TerminationFuture();

  /**
   * Launch the application.
   */
  public static void main(String[] args) {

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          // Set System L&F
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

          MainFormUploaderWindow window = new MainFormUploaderWindow();
          window.frame.setTitle(FORM_UPLOADER_VERSION);
          ImageIcon icon = new ImageIcon(MainFormUploaderWindow.class.getClassLoader().getResource("odk_logo.png"));
          window.frame.setIconImage(icon.getImage());
          window.frame.setVisible(true);
        } catch (Exception e) {
          log.error("failed to launch app", e);
        }
      }
    });
  }
  
  class FormDefinitionActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // briefcase...
      WrappedFileChooser fc = new WrappedFileChooser(MainFormUploaderWindow.this.frame,
          new FormDefinitionFileChooser(MainFormUploaderWindow.this.frame));
      String path = txtFormDefinitionFile.getText();
      if ( path != null && path.trim().length() != 0 ) {
        fc.setSelectedFile(new File(path.trim()));
      }
      int retVal = fc.showDialog();
      if (retVal == JFileChooser.APPROVE_OPTION) {
        if (fc.getSelectedFile() != null) {
          String formDefinitionPath = fc.getSelectedFile().getAbsolutePath();
          try {
            File formFile = new File(formDefinitionPath);
            BriefcaseFormDefinition lfd = new BriefcaseFormDefinition(formFile.getParentFile(), new File(formDefinitionPath));
            fs = new FormStatus(FormStatus.TransferType.UPLOAD, lfd);
            txtFormDefinitionFile.setText(formDefinitionPath);
            setUploadFormEnabled(true);
            lblUploading.setText("");
            btnDetails.setEnabled(false);
          } catch ( BadFormDefinition ex ) {
            log.error("failed to create form definition for upload", ex);
          }
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
      // need to show (modal) connect dialog...
      ServerConnectionDialog d = new ServerConnectionDialog(
          MainFormUploaderWindow.this.frame.getOwner(), 
          destinationServerInfo, true);
      d.setVisible(true);
      if (d.isSuccessful()) {
        ServerConnectionInfo info = d.getServerInfo();
        if (info.isOpenRosaServer()) {
          destinationServerInfo = d.getServerInfo();
          txtDestinationName.setText(destinationServerInfo.getUrl());
          setUploadFormEnabled(true);
          lblUploading.setText("");
          btnDetails.setEnabled(false);
        } else {
          ODKOptionPane.showErrorDialog(MainFormUploaderWindow.this.frame,
              "Server is not an ODK Aggregate 1.0 server", "Invalid Server URL");
        }
      }
    }

  }

  /**
   * Create the application.
   */
  public MainFormUploaderWindow() {
    AnnotationProcessor.process(this);// if not using AOP
    initialize();
  }

  private void setUploadFormEnabled(boolean enable) {
      btnUploadForm.setEnabled(enable &&
        destinationServerInfo != null &&
        txtFormDefinitionFile.getText() != null &&
        txtFormDefinitionFile.getText().length() != 0);
  }
  
  private void setActiveTransferState(boolean active) {
    if (active) {
      // don't allow normal actions when we are transferring...
      btnChoose.setEnabled(false);
      btnChooseServer.setEnabled(false);
      setUploadFormEnabled(false);
      btnClose.setEnabled(false);
      // enable cancel button
      btnCancel.setEnabled(true);
      // show downloading progress text
      lblUploading.setText(UPLOADING_DOT_ETC);
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
      fs.clearStatusHistory();
      btnDetails.setEnabled(true);
    } else {
      // restore normal actions when we aren't transferring...
      btnChoose.setEnabled(true);
      btnChooseServer.setEnabled(true);
      setUploadFormEnabled(true);
      btnClose.setEnabled(true);
      // disable cancel button
      btnCancel.setEnabled(false);
      // leave progress text
      // details are available if we have an fs...
      btnDetails.setEnabled(fs != null);
    }
    // remember state...
    transferStateActive = active;
  }

  private void updateUploadingLabel() {
    String text = lblUploading.getText();
    if (text.equals(UPLOADING_DOT_ETC)) {
      text = "Uploading.";
    } else {
      text += ".";
    }
    lblUploading.setText(text);
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void updateDetailedStatus(FormStatusEvent fse) {
    updateUploadingLabel();
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    lblUploading.setText("Failed!");
    setActiveTransferState(false);
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    lblUploading.setText("Succeeded!");
    setActiveTransferState(false);
  }
  
  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    frame = new JFrame();
    frame.setBounds(100, 100, 680, 206);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JLabel lblFormDefinitionFile = new JLabel("Form Definition to upload:");

    txtFormDefinitionFile = new JTextField();
    txtFormDefinitionFile.setFocusable(false);
    txtFormDefinitionFile.setEditable(false);
    txtFormDefinitionFile.setColumns(10);

    btnChoose = new JButton("Choose...");
    btnChoose.addActionListener(new FormDefinitionActionListener());
    
    txtDestinationName = new JTextField();
    txtDestinationName.setFocusable(false);
    txtDestinationName.setEditable(false);
    txtDestinationName.setColumns(10);
    
    btnChooseServer = new JButton("Configure...");
    btnChooseServer.addActionListener(new DestinationActionListener());
    
    lblUploading = new JLabel("");
    
    btnDetails = new JButton("Details...");
    btnDetails.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if ( fs != null ) {
          ScrollingStatusListDialog.showDialog(
            MainFormUploaderWindow.this.frame, fs.getFormDefinition(), fs.getStatusHistory());
        }
      }});

    btnUploadForm = new JButton("Upload Form");
    btnUploadForm.addActionListener(new ActionListener(){

      @Override
      public void actionPerformed(ActionEvent e) {
        setActiveTransferState(true);
        File formDefn = new File(txtFormDefinitionFile.getText());
        TransferAction.uploadForm(
            MainFormUploaderWindow.this.frame.getOwner(),
            destinationServerInfo, terminationFuture, formDefn, fs);
      }});

    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        terminationFuture.markAsCancelled(
            new TransferAbortEvent("Form upload cancelled by user."));
      }});
    
    btnClose = new JButton("Close");
    btnClose.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if ( transferStateActive ) {
          if ( JOptionPane.YES_OPTION != JOptionPane.showOptionDialog(frame,
              "An upload is in progress. Are you sure you want to abort and exit?",
              "Confirm Stop Form Upload",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.ERROR_MESSAGE, null, null, null) ) {
            return; // no-op
          }
          terminationFuture.markAsCancelled(new TransferAbortEvent("User closes window"));
        }
        frame.setVisible(false);
        frame.dispose();
        System.exit(0);
      }
    });
    
    JLabel lblUploadToServer = new JLabel("Upload to server:");
    
    GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
    groupLayout.setHorizontalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(lblFormDefinitionFile)
            .addComponent(lblUploadToServer)
            .addComponent(lblUploading)
            )
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(txtFormDefinitionFile, 
                GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addComponent(txtDestinationName,  
                GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                .addComponent(btnDetails)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(btnUploadForm)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(btnCancel)))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(btnChoose, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(btnChooseServer, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
          .addComponent(btnClose))
        .addContainerGap());
    groupLayout.setVerticalGroup(
      groupLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(groupLayout.createSequentialGroup()
          .addContainerGap()
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(txtFormDefinitionFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(lblFormDefinitionFile)
            .addComponent(btnChoose))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(lblUploadToServer)
            .addComponent(txtDestinationName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(btnChooseServer))
          .addPreferredGap(ComponentPlacement.UNRELATED, 10, Short.MAX_VALUE)
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
              .addComponent(lblUploading)
              .addComponent(btnDetails)
            .addComponent(btnUploadForm)
            .addComponent(btnCancel)
            .addComponent(btnClose))
          .addContainerGap())
    );

    frame.getContentPane().setLayout(groupLayout);
    setActiveTransferState(false);
  }
}
