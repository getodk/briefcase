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
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class MainBriefcaseWindow {
  private static final String BRIEFCASE_VERSION = "ODK Briefcase - " + BriefcasePreferences.VERSION;

  private JFrame frame;
  private JTextField txtBriefcaseDir;
  private JButton btnChoose;
  private TransferPanel transferPanel;
  private TransformPanel transformPanel;
  private final TerminationFuture transformTerminationFuture = new TerminationFuture();
  private final TerminationFuture transferTerminationFuture = new TerminationFuture();
  
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
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void establishUserBriefcaseScratchSpace() {
    try {
      File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
      File scratchDir = FileSystemUtils.getScratchFolder(briefcaseDir);
      int outcome = 0;
      if (scratchDir.exists() && scratchDir.list() != null && scratchDir.list().length != 0) {
        Object[] options = { "Yes, empty scratch workspace", "No, resume." };
        outcome = JOptionPane.showOptionDialog(frame,
            "Remove temporary files and start with an empty scratch workspace?",
            "ODK Briefcase scratch workspace was not cleared on last exit", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      }
      if (outcome == 0) {
        FileSystemUtils.removeBriefcaseScratch(scratchDir);
      }
      FileSystemUtils.establishBriefcaseScratch(scratchDir);

    } catch (IOException e1) {
      e1.printStackTrace();
      JOptionPane.showMessageDialog(frame,
          "Unable to initialize scratch directory: " + e1.getMessage());
    }
  }
  
  class FolderActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // briefcase...
      BriefcaseFolderChooser fc = new BriefcaseFolderChooser(MainBriefcaseWindow.this.frame, true);
      int retVal = fc.showDialog(MainBriefcaseWindow.this.frame, null);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        if (fc.getSelectedFile() != null) {
          String briefcasePath = fc.getSelectedFile().getAbsolutePath();
          txtBriefcaseDir.setText(briefcasePath);
          BriefcasePreferences.setBriefcaseDirectoryProperty(briefcasePath);
          establishUserBriefcaseScratchSpace();
          transformPanel.setEnabled(true);
          transferPanel.setEnabled(true);
        }
      }
    }

  }

  /**
   * Create the application.
   */
  public MainBriefcaseWindow() {
    initialize();
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    frame = new JFrame();
    frame.setBounds(100, 100, 680, 595);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.addWindowListener(new WindowListener() {
      @Override
      public void windowOpened(WindowEvent e) {
      }

      @Override
      public void windowClosing(WindowEvent e) {
        String briefcaseDir = BriefcasePreferences.getBriefcaseDirectoryProperty();
        if (briefcaseDir != null && briefcaseDir.length() != 0) {
          File scratch = FileSystemUtils.getScratchFolder(new File(briefcaseDir));
          try {
            boolean nonEmpty = scratch.exists() && scratch.listFiles() != null
                && scratch.listFiles().length > 0;
            FileSystemUtils.removeBriefcaseScratch(scratch);
            if (nonEmpty) {
              JOptionPane.showMessageDialog(frame, "Scratch workspace has been cleared");
            }
          } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Unable to remove temporary files and directory: "
                + e1.getMessage());
          }
        }
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

    JLabel lblBriefcaseDirectory = new JLabel("Briefcase Directory");

    txtBriefcaseDir = new JTextField();
    txtBriefcaseDir.setFocusable(false);
    txtBriefcaseDir.setEditable(false);
    txtBriefcaseDir.setColumns(10);
    txtBriefcaseDir.setText(BriefcasePreferences.getBriefcaseDirectoryProperty());

    btnChoose = new JButton("Choose...");
    btnChoose.addActionListener(new FolderActionListener());

    JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
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

    transferPanel = new TransferPanel(transferTerminationFuture);
    tabbedPane.addTab("Transfer", null, transferPanel, null);

    transformPanel = new TransformPanel(transformTerminationFuture);
    tabbedPane.addTab("Transform", null, transformPanel, null);
    frame.getContentPane().setLayout(groupLayout);

    // set the enabled/disabled status of the panels based upon validity of default briefcase directory.
    File f = new File( BriefcasePreferences.getBriefcaseDirectoryProperty());
    if (BriefcaseFolderChooser.testAndMessageBadBriefcaseFolder(f, frame)) {
      establishUserBriefcaseScratchSpace();
      transformPanel.setEnabled(true);
      transferPanel.setEnabled(true);
    } else {
      transformPanel.setEnabled(false);
      transferPanel.setEnabled(false);
    }
  }
}
