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
package org.opendatakit.briefcase.delivery.ui.reused.filsystem;

import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JFileChooser.OPEN_DIALOG;
import static org.opendatakit.briefcase.reused.model.Host.isLinux;
import static org.opendatakit.briefcase.reused.model.Host.isWindows;

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

public interface FileChooser {
  /**
   * Returns whether pathname is a folder under the Briefcase storage folder
   *
   * @param pathname the File to check
   */
  static boolean isUnderBriefcaseFolder(File pathname) {
    File parent = pathname == null ? null : pathname.getParentFile();
    while (parent != null) {
      if (isStorageLocation(parent))
        return true;
      parent = parent.getParentFile();
    }
    return false;
  }

  static boolean isStorageLocation(File path) {
    if (!path.exists())
      return false;
    File forms = new File(path, "forms");
    File db = new File(path, "db");
    return forms.exists() && db.exists() && forms.isDirectory();
  }

  static boolean isODKDevice(File pathname) {
    File fo = new File(pathname, "odk");
    File foi = new File(fo, "instances");
    File fof = new File(fo, "forms");
    return fo.exists() && foi.exists() && fof.exists();
  }

  static boolean isUnderODKFolder(File pathname) {
    File parent = (pathname == null ? null : pathname.getParentFile());
    File current = pathname;
    while (parent != null) {
      if (isODKDevice(parent) && current.getName().equals("odk"))
        return true;
      current = parent;
      parent = parent.getParentFile();
    }
    return false;
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
        : new NativeFileChooser(buildFileDialog(parent, initialLocation, fileChooser), fileChooser, filter, filterDescription);
  }

  static FileChooser file(Container parent, Optional<File> initialFile) {
    return file(parent, initialFile, f -> true, "");
  }

  static FileChooser file(Container parent, Optional<File> initialFile, Predicate<File> filter, String filterDescription) {
    Optional<FileFilter> fileFilter = Optional.ofNullable(filter).map(f -> createFileFilter(f, filterDescription));
    JFileChooser fileChooser = buildFileChooser(initialFile, "Choose a file", FILES_ONLY, fileFilter);

    return isLinux() || isWindows()
        ? new SwingFileChooser(parent, fileChooser, filter, filterDescription)
        : new NativeFileChooser(buildFileDialog(parent, initialFile, fileChooser), fileChooser, filter, filterDescription);
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