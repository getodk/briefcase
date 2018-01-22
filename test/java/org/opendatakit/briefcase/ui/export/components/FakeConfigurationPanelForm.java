package org.opendatakit.briefcase.ui.export.components;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

class FakeConfigurationPanelForm extends ConfigurationPanelForm {
  public boolean errorShown = false;
  public boolean enabled;

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
