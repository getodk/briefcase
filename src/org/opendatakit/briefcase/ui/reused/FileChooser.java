/*
 * Copyright (C) 2018 Nafundi
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
package org.opendatakit.briefcase.ui.reused;

import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JFileChooser.OPEN_DIALOG;
import static org.opendatakit.briefcase.util.FileSystemUtils.FORMS_DIR;
import static org.opendatakit.briefcase.util.Host.isLinux;
import static org.opendatakit.briefcase.util.Host.isWindows;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.opendatakit.briefcase.model.BriefcasePreferences;

public interface FileChooser {
  /**
   * Returns whether pathname is a folder under the Briefcase storage folder
   *
   * @param pathname the File to check
   */
  static boolean isUnderBriefcaseFolder(File pathname) {
    File current = pathname;
    File parent = pathname == null ? null : pathname.getParentFile();
    while (parent != null) {
      if (isStorageLocationParent(parent) && current.getName().equals(BriefcasePreferences.BRIEFCASE_DIR)) {
        return true;
      }
      current = parent;
      parent = parent.getParentFile();
    }
    return false;
  }

  static boolean isStorageLocationParent(File pathname) {
    if (!pathname.exists()) {
      return false;
    }
    File folder = new File(pathname, BriefcasePreferences.BRIEFCASE_DIR);
    if (!folder.exists() || !folder.isDirectory()) {
      return false;
    }
    File forms = new File(folder, FORMS_DIR);
    return forms.exists() && forms.isDirectory();
  }

  Optional<File> choose();

  static FileChooser directory(Container parent, Optional<File> initialLocation) {
    return directory(parent, initialLocation, __ -> true, "");
  }

  static FileChooser directory(Container parent, Optional<File> initialLocation, Predicate<File> filter, String filterDescription) {
    Optional<FileFilter> fileFilter = Optional.ofNullable(filter).map(f -> createFileFilter(f, filterDescription));

    JFileChooser fileChooser = buildFileChooser(initialLocation, "Choose a directory", DIRECTORIES_ONLY, fileFilter);

    return isLinux() || isWindows()
        ? new SwingFileChooser(parent, fileChooser, filter, filterDescription)
        : new NativeFileChooser(parent, buildFileDialog(parent, initialLocation, fileChooser), fileChooser, filter, filterDescription);
  }

  static FileChooser file(Container parent, Optional<File> initialFile) {
    return file(parent, initialFile, f -> true, "");
  }

  static FileChooser file(Container parent, Optional<File> initialFile, Predicate<File> filter, String filterDescription) {
    Optional<FileFilter> fileFilter = Optional.ofNullable(filter).map(f -> createFileFilter(f, filterDescription));
    JFileChooser fileChooser = buildFileChooser(initialFile, "Choose a file", FILES_ONLY, fileFilter);

    return isLinux() || isWindows()
        ? new SwingFileChooser(parent, fileChooser, filter, filterDescription)
        : new NativeFileChooser(parent, buildFileDialog(parent, initialFile, fileChooser), fileChooser, filter, filterDescription);
  }

  static FileDialog buildFileDialog(Container parent, Optional<File> initialLocation, JFileChooser fileChooser) {
    System.setProperty("apple.awt.fileDialogForDirectories", fileChooser.getFileSelectionMode() == DIRECTORIES_ONLY ? "true" : "false");
    Window windowAncestor = SwingUtilities.getWindowAncestor(parent);
    FileDialog fileDialog = windowAncestor instanceof Frame
        ? new FileDialog((Frame) windowAncestor, fileChooser.getDialogTitle())
        : new FileDialog((Dialog) windowAncestor, fileChooser.getDialogTitle());
    if (fileChooser.getFileSelectionMode() == DIRECTORIES_ONLY)
      fileDialog.setFilenameFilter((dir, name) -> new File(dir, name).isDirectory());
    initialLocation.ifPresent(file -> fileDialog.setFile(file.getAbsolutePath()));
    return fileDialog;
  }

  static JFileChooser buildFileChooser(Optional<File> initialLocation, String title, int selectionMode, Optional<FileFilter> fileFilter) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(title);
    fileChooser.setFileSelectionMode(selectionMode);
    fileChooser.setDialogType(OPEN_DIALOG);
    fileFilter.ifPresent(fileChooser::setFileFilter);
    initialLocation.ifPresent(fileChooser::setSelectedFile);
    return fileChooser;
  }

  static FileFilter createFileFilter(Predicate<File> predicate, String description) {
    return new FileFilter() {
      @Override
      public boolean accept(File f) {
        return predicate.test(f);
      }

      @Override
      public String getDescription() {
        return description;
      }
    };
  }
}
