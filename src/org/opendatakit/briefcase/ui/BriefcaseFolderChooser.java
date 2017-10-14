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

import org.opendatakit.briefcase.util.FileSystemUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import java.awt.Container;
import java.io.File;

import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;

class BriefcaseFolderChooser extends AbstractFileChooser {

  private final Container parentWindow;
  private static final long serialVersionUID = 7687033156045655297L;

  /**
   * Returns true if directory contains a valid BriefcaseStorageLocation or could contain one. Shows messages
   * in dialogs.
   */
  static boolean testAndMessageBadBriefcaseStorageLocationParentFolder(File file, Container parentWindow) {
    if (file == null || !file.exists()) {
      ODKOptionPane.showErrorDialog(parentWindow, MessageStrings.DIR_NOT_EXIST,
          MessageStrings.INVALID_BRIEFCASE_STORAGE_LOCATION);
      return true;
    }
    if (isUnderBriefcaseFolder(file)) {
      ODKOptionPane.showErrorDialog(parentWindow, MessageStrings.DIR_INSIDE_BRIEFCASE_STORAGE,
          MessageStrings.INVALID_BRIEFCASE_STORAGE_LOCATION);
      return false;
    }
    if (FileSystemUtils.isUnderODKFolder(file)) {
      ODKOptionPane.showErrorDialog(parentWindow, MessageStrings.DIR_INSIDE_ODK_DEVICE_DIRECTORY,
          MessageStrings.INVALID_BRIEFCASE_STORAGE_LOCATION);
      return false;
    }

    return true;
  }

  @Override
  public void approveSelection() {
    if (testAndMessageBadBriefcaseStorageLocationParentFolder(getSelectedFile(), parentWindow)) {
      super.approveSelection();
    }
  }

  BriefcaseFolderChooser(Container parentWindow) {
    super();
    this.parentWindow = parentWindow;
    setDialogTitle("Choose " + MessageStrings.BRIEFCASE_STORAGE_LOCATION);
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
      }
    });

    setApproveButtonText("Choose");
  }

  @Override
  public boolean testAndMessageBadFolder(File f, Container parentWindow) {
    return testAndMessageBadBriefcaseStorageLocationParentFolder(f, parentWindow);
  }
}