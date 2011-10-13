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
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;
import org.opendatakit.briefcase.util.TransferAction;

public class MainFormUploaderWindow implements ActionListener {
  private static final String FORM_UPLOADER_VERSION = "ODK FormUploader - " + BriefcasePreferences.VERSION;

  private ServerConnectionInfo destinationServerInfo = null;
  private JFrame frame;
  private JTextField txtFormDefinitionFile;
  private JButton btnChoose;
  private JTextField txtDestinationName;

  private JButton btnUploadForm;

  private JButton btnDetails;

  private FormStatus fs = null;

  private JButton btnChooseServer;

  private JLabel lblOutcome;

  private JButton btnClose;

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
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
  
  class FormDefinitionActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // briefcase...
      FormDefinitionFileChooser fc = new FormDefinitionFileChooser(MainFormUploaderWindow.this.frame);
      int retVal = fc.showDialog(MainFormUploaderWindow.this.frame, null);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        if (fc.getSelectedFile() != null) {
          String formDefinitionPath = fc.getSelectedFile().getAbsolutePath();
          try {
            LocalFormDefinition lfd = new LocalFormDefinition(new File(formDefinitionPath));
            fs = new FormStatus(lfd);
            txtFormDefinitionFile.setText(formDefinitionPath);
            btnUploadForm.setEnabled(destinationServerInfo != null);
            btnDetails.setEnabled(false);
          } catch ( BadFormDefinition ex ) {
            ex.printStackTrace();
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
      ServerConnectionDialog d = new ServerConnectionDialog(destinationServerInfo, true);
      d.setVisible(true);
      if (d.isSuccessful()) {
        ServerConnectionInfo info = d.getServerInfo();
        if (info.isOpenRosaServer()) {
          destinationServerInfo = d.getServerInfo();
          txtDestinationName.setText(destinationServerInfo.getUrl());
          btnUploadForm.setEnabled( txtFormDefinitionFile.getText() != null &&
                                    txtFormDefinitionFile.getText().length() != 0);
          btnDetails.setEnabled(false);
        } else {
          JOptionPane.showMessageDialog(MainFormUploaderWindow.this.frame,
              "Server is not an ODK Aggregate 1.0 server", "Invalid Server URL",
              JOptionPane.ERROR_MESSAGE);
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

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    btnUploadForm.setEnabled(true);
    btnClose.setEnabled(true);
    lblOutcome.setText("Failed!");
    btnDetails.setEnabled(true);
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    btnUploadForm.setEnabled(true);
    btnClose.setEnabled(true);
    lblOutcome.setText("Succeeded!");
    btnDetails.setEnabled(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    terminationFuture.markAsCancelled(new TransferAbortEvent("User closes window"));
    frame.setVisible(false);
    frame.dispose();
    System.exit(0);
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
    
    btnUploadForm = new JButton("Upload Form");
    btnUploadForm.addActionListener(new ActionListener(){

      @Override
      public void actionPerformed(ActionEvent e) {
        terminationFuture.reset();
        fs.clearStatusHistory();
        btnUploadForm.setEnabled(false);
        btnClose.setEnabled(false);
        File formDefn = new File(txtFormDefinitionFile.getText());
        TransferAction.uploadForm(destinationServerInfo, terminationFuture, formDefn, fs);
      }});
    
    btnDetails = new JButton("Details...");
    btnDetails.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        ScrollingStatusListDialog.showDialog(MainFormUploaderWindow.this.frame, fs.getFormName(), fs.getStatusHistory());
      }});
    
    btnClose = new JButton("Close");
    btnClose.addActionListener(this);
    
    JLabel lblUploadToServer = new JLabel("Upload to server:");
    
    lblOutcome = new JLabel("");
    lblOutcome.setHorizontalAlignment(SwingConstants.RIGHT);
    
    GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
    groupLayout.setHorizontalGroup(
      groupLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(groupLayout.createSequentialGroup()
          .addContainerGap()
          .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
              .addGroup(groupLayout.createSequentialGroup()
                .addComponent(lblFormDefinitionFile)
                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtFormDefinitionFile, GroupLayout.PREFERRED_SIZE, 315, GroupLayout.PREFERRED_SIZE))
              .addGroup(groupLayout.createSequentialGroup()
                .addComponent(lblUploadToServer)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(txtDestinationName, GroupLayout.PREFERRED_SIZE, 383, GroupLayout.PREFERRED_SIZE)))
            .addGroup(groupLayout.createSequentialGroup()
              .addComponent(btnUploadForm)
              .addGap(45)
              .addComponent(lblOutcome, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
              .addPreferredGap(ComponentPlacement.RELATED)
              .addComponent(btnDetails)))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
            .addComponent(btnChoose, GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
            .addComponent(btnChooseServer, GroupLayout.PREFERRED_SIZE, 107, Short.MAX_VALUE)
            .addComponent(btnClose, GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
          .addContainerGap())
    );
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
          .addGap(25)
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(btnUploadForm)
            .addComponent(btnClose)
            .addComponent(lblOutcome)
            .addComponent(btnDetails))
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    frame.getContentPane().setLayout(groupLayout);
    btnUploadForm.setEnabled(false);
    btnDetails.setEnabled(false);
  }
}
