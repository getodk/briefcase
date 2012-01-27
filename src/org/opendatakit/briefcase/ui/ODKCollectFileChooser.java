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

final class ODKCollectFileChooser extends JFileChooser {

  /**
	 * 
	 */
  private final Container parentWindow;
  /**
	 * 
	 */
  private static final long serialVersionUID = 8457430037255302153L;

  @Override
  public void approveSelection() {
    File f = this.getSelectedFile();
    if ( f == null || !f.exists() ) {
      JOptionPane.showMessageDialog(parentWindow,
          MessageStrings.DIR_NOT_EXIST,
          MessageStrings.INVALID_ODK_DIRECTORY, JOptionPane.ERROR_MESSAGE);
      return;
    }
    File parent = f.getParentFile();
    if ( !f.isDirectory() ) {
      JOptionPane.showMessageDialog(parentWindow,
          MessageStrings.DIR_NOT_DIRECTORY,
          MessageStrings.INVALID_ODK_DIRECTORY, JOptionPane.ERROR_MESSAGE);
    } else if (FileSystemUtils.isUnderBriefcaseFolder(f)) {
      JOptionPane.showMessageDialog(parentWindow,
          MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE,
          MessageStrings.INVALID_ODK_DIRECTORY, JOptionPane.ERROR_MESSAGE);
    } else if (FileSystemUtils.isUnderODKFolder(parent)) {
      JOptionPane.showMessageDialog(parentWindow,
          MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY,
          MessageStrings.INVALID_ODK_DIRECTORY, JOptionPane.ERROR_MESSAGE);
    } else if (!FileSystemUtils.isODKInstancesParentFolder(f)) {
      JOptionPane.showMessageDialog(this.parentWindow,
          MessageStrings.DIR_NOT_ODK_COLLECT_DIRECTORY, 
          MessageStrings.INVALID_ODK_DIRECTORY, JOptionPane.ERROR_MESSAGE);
    } else {
      super.approveSelection();
    }
  }

  ODKCollectFileChooser(Container parentWindow, String startingPath) {
    super(startingPath != null && startingPath.trim().length() != 0 
        ? startingPath : FileSystemUtils.getMountPoint());
    this.parentWindow = parentWindow;
    setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
  }
}