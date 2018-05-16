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

import static org.opendatakit.briefcase.model.FormStatus.TransferType.UPLOAD;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
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
import org.opendatakit.briefcase.model.FormStatus.TransferType;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.push.PushEvent;

public class TransferInProgressDialog extends JDialog implements ActionListener, WindowListener {

  /**
   *
   */
  private static final long serialVersionUID = 5411425417966734421L;
  private final JPanel contentPanel = new JPanel();
  private JLabel lblNewLabel;
  private JButton cancelButton;
  private JTextArea textAreaStatusDetail;
  private TerminationFuture terminationFuture;
  private TransferType transferType;

  /**
   * Create the dialog.
   */
  public TransferInProgressDialog(Window topLevel, TransferType transferType, TerminationFuture terminationFuture) {
    super(topLevel, ((transferType == UPLOAD) ?
        "Push" : "Push") + " in progress...", ModalityType.DOCUMENT_MODAL);
    AnnotationProcessor.process(this);// if not using AOP
    this.transferType = transferType;
    this.terminationFuture = terminationFuture;

    setBounds(100, 100, 450, 261);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      lblNewLabel = new JLabel(((transferType == UPLOAD) ?
          "Push" : "Pull") + " in progress...");
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
    textAreaStatusDetail.setPreferredSize(new Dimension(300, 105));

    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
        gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_contentPanel.createSequentialGroup()
                .addContainerGap()
                .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
                    .addComponent(lblNewLabel)
                    .addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
                        .addComponent(textAreaStatusDetail, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
    );
    gl_contentPanel.setVerticalGroup(
        gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblNewLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
                    .addComponent(cancelButton)
                    .addComponent(textAreaStatusDetail, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addContainerGap())
    );
    contentPanel.setLayout(gl_contentPanel);
  }

  @EventSubscriber(eventClass = PullEvent.Failure.class)
  public void onPullFailure(PullEvent.Failure event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = PushEvent.Failure.class)
  public void onPushFailure(PushEvent.Failure event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = PushEvent.Success.class)
  public void onPushSuccess(PushEvent.Success event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void updateDetailedStatus(FormStatusEvent fse) {
    textAreaStatusDetail.setText(fse.getStatusString());
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void failedRemoteCompletion(RetrieveAvailableFormsFailedEvent event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsSucceededEvent.class)
  public void successfulRemoteCompletion(RetrieveAvailableFormsSucceededEvent event) {
    this.setVisible(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    terminationFuture.markAsCancelled(new TransferAbortEvent("User cancelled transfer."));
    if (transferType == UPLOAD)
      terminationFuture.markAsCancelled(new PushEvent.Abort("User cancelled transfer."));
    else
      terminationFuture.markAsCancelled(new PullEvent.Abort("User cancelled transfer."));
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
    int outcome = JOptionPane.showConfirmDialog(this, "Cancel the in-progress transfer?",
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
