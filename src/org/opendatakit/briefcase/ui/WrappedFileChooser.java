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
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.opendatakit.briefcase.util.FindDirectoryStructure;

/**
 * This peculiar class wraps a JFileChooser and, on OSX, will use an AWT
 * FileDialog instead of the JFileChooser to render the chooser box.
 *
 * @author mitchellsundt@gmail.com
 *
 */
class WrappedFileChooser {

  private final boolean useAwt;
  private boolean directoriesOnly;
  private Container parentWindow;
  private AbstractFileChooser fc;
  private FileDialog dlg;
  private File chosenFile;

  public boolean testAndMessageBadFolder(File f, Container parentWindow) {
    return fc.testAndMessageBadFolder(f, parentWindow);
  }

  WrappedFileChooser(Container parentWindow, AbstractFileChooser fc) {
    this.parentWindow = parentWindow;
    this.fc = fc;
    this.chosenFile = null;
    directoriesOnly = (fc.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY);

    //Though the Mac-specific file chooser is very nice, it no longer functions on Oracle's Java 7.
    //The issue is that the "apple.awt.fileDialogForDirectories" property is no longer supported in
    //Java 7. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7161437, where the issue is
    //marked as resolved, but they seem only to have fixed it in JDK 8.
    String javaVersion = System.getProperty("java.version");
    useAwt = (!((javaVersion.compareTo("1.7") >= 0) && javaVersion.compareTo("1.8") < 0)) &&
             FindDirectoryStructure.isMac();

    if (useAwt) {
      System.setProperty("apple.awt.fileDialogForDirectories",
                          directoriesOnly ? "true" : "false");
      dlg = new FileDialog((Frame) SwingUtilities.getWindowAncestor(parentWindow),
          fc.getDialogTitle());
      if (directoriesOnly) {
        dlg.setFilenameFilter(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return (new File(dir, name).isDirectory());
          }
        });
      }
    }
  }

  public void setSelectedFile(File f) {
    this.chosenFile = f;
  }

  public File getSelectedFile() {
    return chosenFile;
  }

  public int showDialog() {
    File initialFile = chosenFile;
    if (initialFile == null) {
      initialFile = new File(System.getProperty("user.home"));
    }

    if (!useAwt) {
      fc.setSelectedFile(initialFile);
      int ret = fc.showDialog(parentWindow, fc.getApproveButtonText());
      if (ret == JFileChooser.APPROVE_OPTION) {
        chosenFile = fc.getSelectedFile();
      }
      return ret;
    } else {
      dlg.setFile(initialFile.getAbsolutePath());
      dlg.setVisible(true);
      String dir = dlg.getDirectory();
      String pathname = dlg.getFile();
      File f = new File(dir);
      // can't quite tell in AWT what the actual selection is.
      // if the pathname exists, use it.
      if (pathname != null) {
        File t = new File(f, pathname);
        if (t.exists()) {
          f = t;
        }
      }
      if (f != null && fc.testAndMessageBadFolder(f, parentWindow)) {
        chosenFile = f;
        return JFileChooser.APPROVE_OPTION;
      } else {
        return JFileChooser.CANCEL_OPTION;
      }
    }
  }
}