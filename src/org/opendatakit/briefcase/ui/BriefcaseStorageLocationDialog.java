/*
 * Copyright (C) 2012 University of Washington.
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

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class BriefcaseStorageLocationDialog extends JDialog implements ActionListener {
  /**
   * 
   */
  private static final long serialVersionUID = 1930025310721875038L;
  private static final Log log = LogFactory.getLog(BriefcaseStorageLocationDialog.class);

  JEditorPane lblTheBriefcaseStorage;
  private JTextField txtBriefcaseLocation;
  private JButton btnOK;
  private JButton btnQuit;
  private boolean isCancelled = false;

  public BriefcaseStorageLocationDialog(Window app) {
    super(app, MessageStrings.BRIEFCASE_STORAGE_LOCATION_DIALOG_TITLE, 
          ModalityType.DOCUMENT_MODAL);
    setRootPaneCheckingEnabled(true);
    lblTheBriefcaseStorage = new JEditorPane();
    lblTheBriefcaseStorage.setContentType("text/html");
    lblTheBriefcaseStorage.setEditorKit(new HTMLEditorKit());
    lblTheBriefcaseStorage.setOpaque(false);
    //lblTheBriefcaseStorage.setBackground(UIManager.getColor("Panel.background"));
    lblTheBriefcaseStorage.setEnabled(true);
    lblTheBriefcaseStorage.setEditable(false);
    lblTheBriefcaseStorage.setPreferredSize(new Dimension(650,250));
      lblTheBriefcaseStorage.setText(MessageStrings.BRIEFCASE_STORAGE_LOCATION_EXPLANATION_HTML);
      
    JLabel lblBriefcaseStorageLocation = new JLabel(MessageStrings.BRIEFCASE_STORAGE_LOCATION);
    
    txtBriefcaseLocation = new JTextField();
    String directoryPath = BriefcasePreferences.getBriefcaseDirectoryIfSet();
    if ( directoryPath == null || directoryPath.length() == 0) {
      txtBriefcaseLocation.setColumns(10);
    } else {
      txtBriefcaseLocation.setText(directoryPath + File.separator + FileSystemUtils.BRIEFCASE_DIR);
    }
    txtBriefcaseLocation.setEditable(false);
    
    JButton btnChange = new JButton("Change...");
    btnChange.addActionListener(this);
    
    btnOK = new JButton("OK");
    btnOK.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        try {
          String filename = txtBriefcaseLocation.getText();
          if ( filename == null || filename.trim().length() == 0 ) {
            ODKOptionPane.showErrorDialog(BriefcaseStorageLocationDialog.this,
                "Please change the " + MessageStrings.BRIEFCASE_STORAGE_LOCATION + ".",
                MessageStrings.INVALID_BRIEFCASE_STORAGE_LOCATION);
            return;
          }
          File folder = new File(txtBriefcaseLocation.getText());
          File parentFolder = folder.getParentFile();
          FileSystemUtils.assertBriefcaseStorageLocationParentFolder(parentFolder);
          BriefcasePreferences.setBriefcaseDirectoryProperty(parentFolder.getAbsolutePath());
          BriefcaseStorageLocationDialog.this.setVisible(false);
        } catch (FileSystemException e) {
          String msg = "Unable to create " + FileSystemUtils.BRIEFCASE_DIR + ".  Please change the " +
                  MessageStrings.BRIEFCASE_STORAGE_LOCATION + ".";
          log.warn(msg, e);
          ODKOptionPane.showErrorDialog(BriefcaseStorageLocationDialog.this, msg,
                  "Failed to Create " + FileSystemUtils.BRIEFCASE_DIR);
        }
      }
    });
    
    btnQuit = new JButton("Quit");
    btnQuit.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });
    
    GroupLayout groupLayout = new GroupLayout(getContentPane());
    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(lblTheBriefcaseStorage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblBriefcaseStorageLocation)
            .addGroup(groupLayout.createSequentialGroup()
              .addComponent(txtBriefcaseLocation, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addPreferredGap(ComponentPlacement.RELATED)
              .addComponent(btnChange))
            .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                  .addComponent(btnOK)
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addComponent(btnQuit))))
        .addContainerGap()
    );
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblTheBriefcaseStorage, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(lblBriefcaseStorageLocation)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(txtBriefcaseLocation, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(btnChange))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(btnOK)
            .addComponent(btnQuit))
          .addContainerGap());
    getContentPane().setLayout(groupLayout);   
    pack();
  }

  public boolean isCancelled() {
    return isCancelled;
  }
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    WrappedFileChooser fc = new WrappedFileChooser(this.getParent(), 
        new BriefcaseFolderChooser(this.getParent()));
    // figure out the initial directory path...
    String candidateDir = txtBriefcaseLocation.getText();
    File base = null;
    if ( candidateDir == null || candidateDir.trim().length() == 0 ) {
      // nothing -- use default
      base = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    } else {
      // start with candidate parent and move up the tree until we have a valid directory.
      base = new File(candidateDir).getParentFile();
      while ( base != null && (!base.exists() || !base.isDirectory()) ) {
        base = base.getParentFile();
      }
    }
    if ( base != null ) {
      fc.setSelectedFile(base);
    }
    int retVal = fc.showDialog();
    if (retVal == JFileChooser.APPROVE_OPTION) {
      File parentFolder = fc.getSelectedFile();
      if (parentFolder != null) {
        String briefcasePath = parentFolder.getAbsolutePath();
        txtBriefcaseLocation.setText(briefcasePath + File.separator + FileSystemUtils.BRIEFCASE_DIR);
      }
    }
  }

  public void updateForSettingsPage() {
    btnQuit.setText("Cancel");
    btnQuit.removeActionListener(btnQuit.getActionListeners()[0]);
    btnQuit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
          isCancelled = true;
          BriefcaseStorageLocationDialog.this.setVisible(false);
      }
    });
  }
}
