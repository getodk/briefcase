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

import java.awt.Container;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opendatakit.briefcase.util.FileSystemUtils;

class BriefcaseFolderChooser extends JFileChooser {

  /**
	 * 
	 */
  private final Container parentWindow;
  /**
	 * 
	 */
  private static final long serialVersionUID = 7687033156045655297L;

  /**
   * 
   * @param f
   * @param parentWindow
   * @return true if directory is a valid briefcase directory.
   */
  public static final boolean testAndMessageBadBriefcaseFolder(File f, Container parentWindow) {
    if ( !f.exists() ) {
      return true;
    }
    if (!FileSystemUtils.isValidBriefcaseFolder(f)) {
      JOptionPane.showMessageDialog(parentWindow,
          "Not a Briefcase directory (does not exist, not empty or did not find " + File.separator
              + "forms and/or " + File.separator + "scratch directories)",
          "Invalid Briefcase Directory", JOptionPane.ERROR_MESSAGE);
      return false;
    } else if (FileSystemUtils.isUnderODKFolder(f) || FileSystemUtils.isUnderBriefcaseFolder(f)) {
      JOptionPane.showMessageDialog(parentWindow,
          "Directory appears to be nested within an enclosing Briefcase or ODK Device directory",
          "Invalid Briefcase Directory", JOptionPane.ERROR_MESSAGE);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public void approveSelection() {
    File f = this.getSelectedFile();
    if (testAndMessageBadBriefcaseFolder(f, parentWindow)) {
      super.approveSelection();
    }
  }

  @SuppressWarnings("unused")
  private BriefcaseFolderChooser(Container parentWindow) {
    super();
    this.parentWindow = parentWindow;
  }

  BriefcaseFolderChooser(Container parentWindow, boolean asOpenDialog) {
    super();
    this.parentWindow = parentWindow;
    setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    setDialogType(JFileChooser.SAVE_DIALOG); // allow creating file
    setApproveButtonText(asOpenDialog ? "Open" : "Save");
  }
}