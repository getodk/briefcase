package org.opendatakit.briefcase.ui.reused;

import static javax.swing.JFileChooser.APPROVE_OPTION;

import java.awt.Container;
import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;
import org.opendatakit.briefcase.ui.ODKOptionPane;

class SwingFileChooser implements FileChooser {
  private final Container parent;
  private final JFileChooser fc;
  private Predicate<File> filter;
  private String filterDescription;

  SwingFileChooser(Container parent, JFileChooser fc, Predicate<File> filter, String filterDescription) {
    this.parent = parent;
    this.fc = fc;
    this.filter = filter;
    this.filterDescription = filterDescription;
  }

  @Override
  public Optional<File> choose() {
    if (fc.showDialog(parent, fc.getApproveButtonText()) == APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (file == null)
        return Optional.empty();

      if (!file.exists()) {
        ODKOptionPane.showErrorDialog(parent, "Selected location doesn't exist", "Export configuration error");
        return Optional.empty();
      }

      if (!filter.test(file)) {
        ODKOptionPane.showErrorDialog(parent, "Selected location doesn't comply with filter: " + filterDescription, "Export configuration error");
        return Optional.empty();
      }

      return Optional.of(file);
    }
    return Optional.empty();
  }

}
