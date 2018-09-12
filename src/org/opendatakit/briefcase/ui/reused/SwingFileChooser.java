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

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.awt.Container;
import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFileChooser;

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
        errorMessage("Export configuration error", "Selected location doesn't exist");
        return Optional.empty();
      }

      if (!filter.test(file)) {
        errorMessage("Export configuration error", "Selected location doesn't comply with filter: " + filterDescription);
        return Optional.empty();
      }

      return Optional.of(file);
    }
    return Optional.empty();
  }

}
