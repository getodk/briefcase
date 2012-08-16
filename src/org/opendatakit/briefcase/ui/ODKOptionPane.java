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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Enhanced JOptionPane.showMessageDialog() for a JOptionPane.ERROR_MESSAGE.
 * Line and word-wraps the error message, and places it inside a scroll
 * region so that if the resulting message is longer than 200 characters, 
 * it can still be viewed.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class ODKOptionPane {

  public static void showErrorDialog(Component parentComponent,
        String errorString, String title ) {

    // create a n-character wide label for aiding layout calculations...
    // the dialog box will display this width of text.
    JLabel t = new JLabel("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");
    
    JTextArea textArea = new JTextArea(errorString);
    textArea.setEditable(false);
    textArea.setFont(t.getFont()); // same as JLabel
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setBackground(t.getBackground()); // same as JLabel
    // textArea.setBackground(Color.white); // same as JLabel
    textArea.setForeground(t.getForeground()); // same as JLabel
    
    final JScrollPane scrollPane = new JScrollPane(textArea);
    
    // don't show the gray border of the scroll pane
    // unless we are showing the scroll bar, in which case we do show it.
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.getVerticalScrollBar().addComponentListener(new ComponentListener() {
      @Override
      public void componentHidden(ComponentEvent component) {
        if ( component.getComponent().equals(scrollPane.getVerticalScrollBar()) ) {
          scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
      }

      @Override
      public void componentMoved(ComponentEvent component) {
      }

      @Override
      public void componentResized(ComponentEvent component) {
      }

      @Override
      public void componentShown(ComponentEvent component) {
        if ( component.getComponent().equals(scrollPane.getVerticalScrollBar()) ) {
          scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
      }});
    
    // set preferred and minimum widths of the scroll pane to 
    // the width of the 't' label above with 5 lines within the scroll area.
    Dimension pref = t.getPreferredSize();
    pref.setSize(pref.getWidth(), 5.3*pref.getHeight());
    scrollPane.setMinimumSize(pref);
    scrollPane.setPreferredSize(pref);
    JOptionPane.showMessageDialog(parentComponent,
        scrollPane, title, JOptionPane.ERROR_MESSAGE );
  }
}
