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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.IFormDefinition;

public class ScrollingStatusListDialog extends JDialog implements ActionListener {

  /**
   * 
   */
  private static final long serialVersionUID = 3565952263140071560L;
  private static final Log log = LogFactory.getLog(ScrollingStatusListDialog.class.getName());

  private final JEditorPane editorArea;
  private final IFormDefinition form;

  /**
   * Set up and show the dialog. The first Component argument determines which
   * frame the dialog depends on; it should be a component in the dialog's
   * controlling frame. The second Component argument should be null if you want
   * the dialog to come up with its left corner in the center of the screen;
   * otherwise, it should be the component on top of which the dialog should
   * appear.
   */
  public static void showDialog(Frame frame, IFormDefinition form, String statusHtml) {
    ScrollingStatusListDialog dialog = new ScrollingStatusListDialog(frame, "Detailed Status:",
        "Detailed Transfer Status for ", form, statusHtml);
    dialog.setVisible(true);
  }
  
  public static void showExportDialog(Frame frame, IFormDefinition form, String dirName, String statusHtml) {
    ScrollingStatusListDialog dialog = new ScrollingStatusListDialog(frame,"Export Directory: " + dirName,
        "Export Details for ", form, statusHtml);
    dialog.setVisible(true);
  }

  private ScrollingStatusListDialog(Frame frame, String labelText, String title, IFormDefinition form, String statusHtml) {
    super(frame, title + form.getFormName(), true);
    this.form = form;
    AnnotationProcessor.process(this);
    // Create and initialize the buttons.
    JButton cancelButton = new JButton("Close");
    cancelButton.addActionListener(this);
    getRootPane().setDefaultButton(cancelButton);

    editorArea = new JEditorPane("text/plain", statusHtml);
    editorArea.setEditable(false);
    //Put the editor pane in a scroll pane.
    JScrollPane editorScrollPane = new JScrollPane(editorArea);
    editorScrollPane.setVerticalScrollBarPolicy(
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    editorScrollPane.setPreferredSize(new Dimension(400, 300));
    editorScrollPane.setMinimumSize(new Dimension(10, 10));

    // Create a container so that we can add a title around
    // the scroll pane. Can't add a title directly to the
    // scroll pane because its background would be white.
    // Lay out the label and scroll pane from top to bottom.
    JPanel listPane = new JPanel();
    listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
    JLabel label = new JLabel(labelText);
    label.setLabelFor(editorScrollPane);
    listPane.add(label);
    listPane.add(Box.createRigidArea(new Dimension(0, 5)));
    listPane.add(editorScrollPane);
    listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Lay out the buttons from left to right.
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(cancelButton);

    // Put everything together, using the content pane's BorderLayout.
    Container contentPane = getContentPane();
    contentPane.add(listPane, BorderLayout.CENTER);
    contentPane.add(buttonPane, BorderLayout.PAGE_END);

    // Initialize values.
    pack();
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onEvent(FormStatusEvent event) {
    // Since there can be multiple FormStatusEvent's published concurrently,
    // we have to check if the event is meant for this dialog instance.
    if (isShowing() && event.getStatus().getFormDefinition().equals(form)) {
      try {
        editorArea.read(new StringReader(event.getStatus().getStatusHistory()), null);
      } catch (IOException e) {
        log.warn("failed to load history", e);
      }
    }
  }
  
  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void onEvent(ExportProgressEvent event) {
    appendToDocument(editorArea, event.getText());
  }
  
  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void onEvent(ExportFailedEvent event) {
    appendToDocument(editorArea,"FAILED!");
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void onEvent(ExportSucceededEvent event) {
    appendToDocument(editorArea,"SUCCEEDED!");
  }

  private void appendToDocument(JTextComponent component, String msg) {
    Document doc = component.getDocument();
    try {
      doc.insertString(doc.getLength(), "\n" + msg, null);
    } catch(BadLocationException e) {
      log.error("Insertion failed: " + e.getMessage());
    }
  }

  // Handle clicks on the Set and Cancel buttons.
  public void actionPerformed(ActionEvent e) {
    setVisible(false);
  }
}
