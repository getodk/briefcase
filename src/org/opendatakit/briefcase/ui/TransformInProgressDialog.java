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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransformAbortEvent;
import org.opendatakit.briefcase.model.TransformFailedEvent;
import org.opendatakit.briefcase.model.TransformProgressEvent;
import org.opendatakit.briefcase.model.TransformSucceededEvent;

public class TransformInProgressDialog extends JDialog implements ActionListener, WindowListener {

  /**
	 * 
	 */
  private static final long serialVersionUID = 5411425417966734421L;
  private final JPanel contentPanel = new JPanel();
  private JLabel lblNewLabel;
  private JButton cancelButton;
  private JTextArea textAreaStatusDetail;
  private TerminationFuture terminationFuture;

  /**
   * Create the dialog.
   */
  public TransformInProgressDialog(String label, TerminationFuture terminationFuture) {
    super(null, "Transformations in progress...", ModalityType.DOCUMENT_MODAL);
    AnnotationProcessor.process(this);// if not using AOP
    this.terminationFuture = terminationFuture;

    setBounds(100, 100, 450, 261);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      lblNewLabel = new JLabel(label);
    }
    cancelButton = new JButton("Cancel");
    cancelButton.setActionCommand("Cancel");
    cancelButton.addActionListener(this);
    addWindowListener(this);
    
    textAreaStatusDetail = new JTextArea();
    textAreaStatusDetail.setFocusable(false);
    textAreaStatusDetail.setFocusTraversalKeysEnabled(false);
    textAreaStatusDetail.setEditable(false);
    textAreaStatusDetail.setFont(UIManager.getFont("Label.font"));
    textAreaStatusDetail.setBackground(UIManager.getColor("Label.background"));
    textAreaStatusDetail.setLineWrap(true);
    textAreaStatusDetail.setWrapStyleWord(true);
    
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
              .addComponent(textAreaStatusDetail, GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
              .addGap(18)
              .addComponent(cancelButton))
            .addComponent(lblNewLabel))
          .addContainerGap())
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblNewLabel)
          .addPreferredGap(ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addComponent(cancelButton)
            .addComponent(textAreaStatusDetail, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE))
          .addGap(26))
    );
    contentPanel.setLayout(gl_contentPanel);
  }

  @EventSubscriber(eventClass = TransformFailedEvent.class)
  public void failedCompletion(TransformFailedEvent event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = TransformSucceededEvent.class)
  public void successfulCompletion(TransformSucceededEvent event) {
    this.setVisible(false);
  }
  
  @EventSubscriber(eventClass = TransformProgressEvent.class)
  public void updateDetailedStatus(TransformProgressEvent se) {
    textAreaStatusDetail.setText(se.getText());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    terminationFuture.markAsCancelled(new TransformAbortEvent("User cancelled transformation."));
    cancelButton.setEnabled(false);
  }

  @Override
  public void windowOpened(WindowEvent e) {
  }

  @Override
  public void windowClosing(WindowEvent e) {
    // if the user attempts to close the window,
    // warn that this will stop the transfer, and
    // if they still want to, do the same action
    // as the cancel button.
    int outcome = JOptionPane.showConfirmDialog(this, "Cancel the in-progress transformation?",
        "Close Window", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (outcome == JOptionPane.OK_OPTION) {
      actionPerformed(null);
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
}
