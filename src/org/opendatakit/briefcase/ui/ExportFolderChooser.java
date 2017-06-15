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
import javax.swing.filechooser.FileFilter;

import org.opendatakit.briefcase.util.FileSystemUtils;

/**
 * Class that ensures that a non-briefcase directory is chosen
 * @author mitchellsundt@gmail.com
 *
 */
class ExportFolderChooser extends AbstractFileChooser {

  /**
     * 
     */
  private final Container parentWindow;
  /**
     * 
     */
  private static final long serialVersionUID = 7687033156145655297L;
  
  /**
   * 
   * @param f
   * @param parentWindow
   * @return true if directory is a valid briefcase directory.
   */
  public static final boolean testAndMessageBadNonBriefcaseFolder(File f, Container parentWindow) {
    if ( !f.exists() ) {
      return false;
    } else if (!f.isDirectory()) {
      ODKOptionPane.showErrorDialog(parentWindow,
          MessageStrings.DIR_NOT_DIRECTORY,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return false;
    } else if (FileSystemUtils.isUnderBriefcaseFolder(f)) {
      ODKOptionPane.showErrorDialog(parentWindow,
          MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
      return false;
    } else if (FileSystemUtils.isUnderODKFolder(f)) {
      ODKOptionPane.showErrorDialog(parentWindow,
          MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY,
          MessageStrings.INVALID_EXPORT_DIRECTORY);
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

  public ExportFolderChooser(Container parentWindow) {
    super();
    this.parentWindow = parentWindow;
    setDialogTitle("Choose " + MessageStrings.EXPORT_DIRECTORY);
    setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    setDialogType(JFileChooser.OPEN_DIALOG); // allow creating file
    setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.isDirectory();
      }

      @Override
      public String getDescription() {
        return "Directories";
      }});
    
    setApproveButtonText("Choose");
  }

  @Override
  public boolean testAndMessageBadFolder(File f, Container parentWindow) {
    return testAndMessageBadNonBriefcaseFolder(f, parentWindow);
  }
}