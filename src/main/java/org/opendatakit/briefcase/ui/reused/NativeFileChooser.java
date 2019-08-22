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

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.awt.FileDialog;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;

class NativeFileChooser implements FileChooser {
  private final FileDialog fileDialog;
  private JFileChooser fileChooser;
  private Predicate<File> filter;
  private String filterDescription;

  NativeFileChooser(FileDialog fileDialog, JFileChooser fileChooser, Predicate<File> filter, String filterDescription) {
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
      errorMessage("Export configuration error", "Selected location doesn't exist");
      return Optional.empty();
    }

    if (!filter.test(path.toFile())) {
      errorMessage("Export configuration error", "Selected location doesn't comply with filter: " + filterDescription);
      return Optional.empty();
    }

    return Optional.of(path.toFile());
  }
}
