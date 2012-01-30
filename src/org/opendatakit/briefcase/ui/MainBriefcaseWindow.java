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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportAbortEvent;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class MainBriefcaseWindow implements WindowListener {
  private static final String BRIEFCASE_VERSION = "ODK Briefcase - " + BriefcasePreferences.VERSION;

  private JFrame frame;
  private JTextField txtBriefcaseDir;
  private JButton btnChoose;
  private PullTransferPanel gatherPanel;
  private PushTransferPanel uploadPanel;
  private ExportPanel exportPanel;
  private final TerminationFuture exportTerminationFuture = new TerminationFuture();
  private final TerminationFuture transferTerminationFuture = new TerminationFuture();

  private JTabbedPane tabbedPane;
  
  /**
   * Launch the application.
   */
  public static void main(String[] args) {

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          // Set System L&F
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

          MainBriefcaseWindow window = new MainBriefcaseWindow();
          window.frame.setTitle(BRIEFCASE_VERSION);
          ImageIcon icon = new ImageIcon(
              MainBriefcaseWindow.class.getClassLoader().getResource("odk_logo.png"));
          window.frame.setIconImage(icon.getImage());
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
  
  class FolderActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // briefcase...
      establishBriefcaseStorageLocation(true);
    }

  }

  void setFullUIEnabled(boolean state) {
    String path = BriefcasePreferences.getBriefcaseDirectoryIfSet();
    if ( path != null ) {
      txtBriefcaseDir.setText(path + File.separator + FileSystemUtils.BRIEFCASE_DIR);
    } else {
      txtBriefcaseDir.setText("");
    }
    if ( state ) {
      exportPanel.updateComboBox();
      uploadPanel.updateFormStatuses();
      exportPanel.setEnabled(true);
      gatherPanel.setEnabled(true);
      uploadPanel.setEnabled(true);
      tabbedPane.setEnabled(true);
    } else {
      exportPanel.setEnabled(false);
      gatherPanel.setEnabled(false);
      uploadPanel.setEnabled(false);
      tabbedPane.setEnabled(false);
    }
  }
  
  /**
   * Create the application.
   */
  public MainBriefcaseWindow() {
    frame = new JFrame();
    frame.setBounds(100, 100, 680, 595);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.addWindowListener(new WindowListener() {
      @Override
      public void windowOpened(WindowEvent e) {
      }

      @Override
      public void windowClosing(WindowEvent e) {
      }

      @Override
      public void windowClosed(WindowEvent e) {
      }

      @Override
      public void windowIconified(WindowEvent e) {
      }

      @Override
      public void windowDeiconified(WindowEvent e) {
      }

      @Override
      public void windowActivated(WindowEvent e) {
      }

      @Override
      public void windowDeactivated(WindowEvent e) {
      }
    });

    JLabel lblBriefcaseDirectory = new JLabel(MessageStrings.BRIEFCASE_STORAGE_LOCATION);

    txtBriefcaseDir = new JTextField();
    txtBriefcaseDir.setFocusable(false);
    txtBriefcaseDir.setEditable(false);
    txtBriefcaseDir.setColumns(10);

    btnChoose = new JButton("Change...");
    btnChoose.addActionListener(new FolderActionListener());

    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
        groupLayout
            .createSequentialGroup()
            .addContainerGap()
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.LEADING)
                    .addComponent(tabbedPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 628,
                        Short.MAX_VALUE)
                    .addGroup(
                        groupLayout
                            .createSequentialGroup()
                            .addComponent(lblBriefcaseDirectory)
                            .addGap(18)
                            .addComponent(txtBriefcaseDir, GroupLayout.DEFAULT_SIZE, 362,
                                Short.MAX_VALUE).addGap(18).addComponent(btnChoose)))
            .addContainerGap()));
    groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
        groupLayout
            .createSequentialGroup()
            .addContainerGap()
            .addGroup(
                groupLayout
                    .createParallelGroup(Alignment.BASELINE)
                    .addComponent(txtBriefcaseDir, GroupLayout.PREFERRED_SIZE,
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnChoose).addComponent(lblBriefcaseDirectory)).addGap(33)
            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
            .addContainerGap()));

    gatherPanel = new PullTransferPanel(transferTerminationFuture);
    tabbedPane.addTab(PullTransferPanel.TAB_NAME, null, gatherPanel, null);
    PullTransferPanel.TAB_POSITION = 0;

    uploadPanel = new PushTransferPanel(transferTerminationFuture);
    tabbedPane.addTab(PushTransferPanel.TAB_NAME, null, uploadPanel, null);
    PushTransferPanel.TAB_POSITION = 1;

    exportPanel = new ExportPanel(exportTerminationFuture);
    tabbedPane.addTab(ExportPanel.TAB_NAME, null, exportPanel, null);
    frame.getContentPane().setLayout(groupLayout);
    ExportPanel.TAB_POSITION = 2;
    
    frame.addWindowListener(this);
    setFullUIEnabled(false);
  }
  
  public void establishBriefcaseStorageLocation(boolean showDialog) {
    // set the enabled/disabled status of the panels based upon validity of default briefcase directory.
    String briefcaseDir = BriefcasePreferences.getBriefcaseDirectoryIfSet();
    boolean reset = false;
    if ( briefcaseDir == null ) {
      reset = true;
    } else {
      File dir = new File(briefcaseDir);
      if ( !dir.exists() || !dir.isDirectory()) {
        reset = true;
      } else { 
        File folder = FileSystemUtils.getBriefcaseFolder();
        if ( !folder.exists() || !folder.isDirectory()) {
          reset = true;
        }
        
      }
    }
    
    if ( showDialog || reset ) {
      // ask for new briefcase location...
      BriefcaseStorageLocationDialog fs =
          new BriefcaseStorageLocationDialog(MainBriefcaseWindow.this.frame);
      fs.setVisible(true);
      if ( fs.isCancelled() ) {
        // if we need to reset the briefcase location, 
        // and have cancelled, then disable the UI.
        // otherwise the value we have is fine.
        setFullUIEnabled(!reset);
      } else {
        String briefcasePath = BriefcasePreferences.getBriefcaseDirectoryIfSet();
        if ( briefcasePath == null ) {
          // we had a bad path -- disable all but Choose...
          setFullUIEnabled(false);
        } else {
          setFullUIEnabled(true);
        }
      }
    } else {
      File f = new File( BriefcasePreferences.getBriefcaseDirectoryProperty());
      if (BriefcaseFolderChooser.testAndMessageBadBriefcaseStorageLocationParentFolder(f, frame)) {
        try {
          FileSystemUtils.assertBriefcaseStorageLocationParentFolder(f);
          setFullUIEnabled(true);
        } catch (FileSystemException e1) {
          e1.printStackTrace();
          JOptionPane.showMessageDialog(frame,
              "Unable to create " + FileSystemUtils.BRIEFCASE_DIR,
              "Failed to Create " + FileSystemUtils.BRIEFCASE_DIR, JOptionPane.ERROR_MESSAGE);
          // we had a bad path -- disable all but Choose...
          setFullUIEnabled(false);
        }
      } else {
        // we had a bad path -- disable all but Choose...
        setFullUIEnabled(false);
      }
    }
  }

  @Override
  public void windowActivated(WindowEvent arg0) {
    // NO-OP
  }

  @Override
  public void windowClosed(WindowEvent arg0) {
    // NO-OP
  }

  @Override
  public void windowClosing(WindowEvent arg0) {
    exportTerminationFuture.markAsCancelled(new ExportAbortEvent("Main window closed"));
    transferTerminationFuture.markAsCancelled(new TransferAbortEvent("Main window closed"));
  }

  @Override
  public void windowDeactivated(WindowEvent arg0) {
    // NO-OP
  }

  @Override
  public void windowDeiconified(WindowEvent arg0) {
    // NO-OP
  }

  @Override
  public void windowIconified(WindowEvent arg0) {
    // NO-OP
  }

  @Override
  public void windowOpened(WindowEvent arg0) {
    establishBriefcaseStorageLocation(false);
  }
}
