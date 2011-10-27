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

/**
 * Class that ensures that a non-briefcase directory is chosen
 * @author mitchellsundt@gmail.com
 *
 */
class NonBriefcaseFolderChooser extends JFileChooser {

  /**
	 * 
	 */
  private final Container parentWindow;
  /**
	 * 
	 */
  private static final long serialVersionUID = 7687033156145655297L;

  public static final boolean isValidNonBriefcaseFolder( String path ) {
	  
	  if ( path == null || path.trim().length() == 0 ) return false;
	  
	  File f = new File(path);
	  if ( !f.exists() ) return false;
	  if ( FileSystemUtils.isUnderODKFolder(f) 
			  || FileSystemUtils.isUnderBriefcaseFolder(f)) return false;
	  if ( !f.isDirectory() ) return false;
	  
	  return true;
  }
  
  /**
   * 
   * @param f
   * @param parentWindow
   * @return true if directory is a valid briefcase directory.
   */
  public static final boolean testAndMessageBadNonBriefcaseFolder(File f, Container parentWindow) {
    if ( !f.exists() ) {
      return true;
    }
    if (FileSystemUtils.isUnderODKFolder(f) || FileSystemUtils.isUnderBriefcaseFolder(f)) {
      JOptionPane.showMessageDialog(parentWindow,
          "Directory appears to be nested within an enclosing Briefcase or ODK Device directory",
          "Invalid Directory", JOptionPane.ERROR_MESSAGE);
      return false;
    } else if (!f.isDirectory()) {
          JOptionPane.showMessageDialog(parentWindow,
              "Not a directory",
              "Invalid Directory", JOptionPane.ERROR_MESSAGE);
      return false;
    } else { 
      return true; // allow directory to already have files and directories...
    }
  }

  @Override
  public void approveSelection() {
    File f = this.getSelectedFile();
    if (testAndMessageBadNonBriefcaseFolder(f, parentWindow)) {
      super.approveSelection();
    }
  }

  @SuppressWarnings("unused")
  private NonBriefcaseFolderChooser(Container parentWindow) {
    super();
    this.parentWindow = parentWindow;
  }

  NonBriefcaseFolderChooser(Container parentWindow, boolean asOpenDialog) {
    super();
    this.parentWindow = parentWindow;
    setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    setDialogType(JFileChooser.SAVE_DIALOG); // allow creating file
    setApproveButtonText(asOpenDialog ? "Open" : "Save");
  }
}