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
package org.opendatakit.briefcase.ui.export.components;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

class FakeConfigurationPanelForm extends ConfigurationPanelForm {
  public boolean errorShown = false;
  public boolean enabled;

  FakeConfigurationPanelForm(boolean clearableExportDir) {
    super(clearableExportDir);
  }

  @Override
  protected void showError(String message, String title) {
    errorShown = true;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Path getExportDir() {
    return Paths.get(exportDirField.getText());
  }

  public Path getPemFile() {
    return Paths.get(pemFileField.getText());
  }

  public LocalDate getDateRangeStart() {
    return LocalDate.of(
        startDatePicker.getDate().getYear(),
        startDatePicker.getDate().getMonthValue(),
        startDatePicker.getDate().getDayOfMonth()
    );
  }

  public LocalDate getDateRangeEnd() {
    return LocalDate.of(
        endDatePicker.getDate().getYear(),
        endDatePicker.getDate().getMonthValue(),
        endDatePicker.getDate().getDayOfMonth()
    );
  }
}
