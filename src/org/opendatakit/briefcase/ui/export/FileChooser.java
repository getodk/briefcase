package org.opendatakit.briefcase.ui.export;

import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JFileChooser.OPEN_DIALOG;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.isUnix;

import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

interface FileChooser {
  Optional<File> choose();

  static FileChooser directory(Container parent, Optional<File> initialLocation, Predicate<File> filter, String filterDescription) {
    Optional<FileFilter> fileFilter = Optional.ofNullable(filter).map(f -> createFileFilter(f, filterDescription));

    JFileChooser fileChooser = buildFileChooser(initialLocation, "Choose a directory", DIRECTORIES_ONLY, fileFilter);

    return isUnix()
        ? new SwingFileChooser(parent, fileChooser, filter, filterDescription)
        : new NativeFileChooser(parent, buildFileDialog(parent, initialLocation, fileChooser), fileChooser, filter, filterDescription);
  }

  static FileChooser file(Container parent, Optional<File> initialFile) {
    JFileChooser fileChooser = buildFileChooser(initialFile, "Choose a file", FILES_ONLY, Optional.empty());

    return isUnix()
        ? new SwingFileChooser(parent, fileChooser, f -> true, "")
        : new NativeFileChooser(parent, buildFileDialog(parent, initialFile, fileChooser), fileChooser, __ -> true, "");
  }

  static FileDialog buildFileDialog(Container parent, Optional<File> initialLocation, JFileChooser fileChooser) {
    System.setProperty("apple.awt.fileDialogForDirectories", fileChooser.getFileSelectionMode() == DIRECTORIES_ONLY ? "true" : "false");
    FileDialog fileDialog = new FileDialog((Frame) SwingUtilities.getWindowAncestor(parent), fileChooser.getDialogTitle());
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
