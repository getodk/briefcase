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
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;

public class MainClearBriefcasePreferencesWindow {

  private static final String CLEAR_PREFERENCES_VERSION =
      "ODK ClearBriefcasePreferences - " + BriefcasePreferences.VERSION;
  private static final Log log = LogFactory.getLog(MainClearBriefcasePreferencesWindow.class);

  /**
   * Launch the application.
   */
  public static void main(String[] args) {

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          // Set System L&F
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          Image myImage = Toolkit.getDefaultToolkit()
              .getImage(MainClearBriefcasePreferencesWindow.class.getClassLoader().getResource("odk_logo.png"));
          Object[] options = {"Purge", "Cancel"};
          int outcome = JOptionPane.showOptionDialog(null,
              "Clear all Briefcase preferences.                                            ", CLEAR_PREFERENCES_VERSION,
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, new ImageIcon(myImage), options, options[1]);
          if (outcome == 0) {
            BriefcasePreferences.setBriefcaseDirectoryProperty(null);
          }
        } catch (Exception e) {
          log.error("failed to launch app", e);
        }
      }
    });
  }
}
