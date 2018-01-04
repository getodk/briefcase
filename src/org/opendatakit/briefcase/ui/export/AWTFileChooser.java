package org.opendatakit.briefcase.ui.export;

import static javax.swing.JFileChooser.FILES_ONLY;

import java.awt.Container;
import java.awt.FileDialog;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;
import org.opendatakit.briefcase.ui.ODKOptionPane;

class AWTFileChooser implements FileChooser {
  private Container parent;
  private final FileDialog fileDialog;
  private JFileChooser fileChooser;
  private Predicate<File> filter;
  private String filterDescription;

  AWTFileChooser(Container parent, FileDialog fileDialog, JFileChooser fileChooser, Predicate<File> filter, String filterDescription) {
    this.parent = parent;
    this.fileDialog = fileDialog;
    this.fileChooser = fileChooser;
    this.filter = filter;
    this.filterDescription = filterDescription;
  }

  @Override
  public Optional<File> choose() {
    fileDialog.setVisible(true);

    if (fileDialog.getDirectory() == null)
      return Optional.empty();

    if (fileChooser.getFileSelectionMode() == FILES_ONLY && fileDialog.getFile() == null)
      return Optional.empty();

    Path path = Paths.get(fileDialog.getDirectory()).resolve(fileDialog.getFile());
    if (!Files.exists(path)) {
      ODKOptionPane.showErrorDialog(parent, "Selected location doesn't exist", "Export configuration error");
      return Optional.empty();
    }

    if (!filter.test(path.toFile())) {
      ODKOptionPane.showErrorDialog(parent, "Selected location doesn't comply with filter: " + filterDescription, "Export configuration error");
      return Optional.empty();
    }

    return Optional.of(path.toFile());
  }
}
