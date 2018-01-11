package org.opendatakit.briefcase.ui.export.components;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ConfigurationPanelTest {
  @Test
  public void it_wires_UI_fields_to_the_model() {
    ExportConfiguration expectedConfiguration = ExportConfiguration.empty();
    expectedConfiguration.setExportDir(Paths.get("/some/path"));
    expectedConfiguration.setPemFile(Paths.get("/some/file.pem"));
    expectedConfiguration.setDateRangeStart(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setDateRangeEnd(LocalDate.of(2019, 1, 1));
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    view.setExportDir(expectedConfiguration.getExportDir().get());
    view.setPemFile(expectedConfiguration.getPemFile().get());
    view.setDateRangeStart(expectedConfiguration.getDateRangeStart().get());
    view.setDateRangeEnd(expectedConfiguration.getDateRangeEnd().get());

    assertThat(panel.getConfiguration(), equalTo(expectedConfiguration));
  }

  @Test
  public void it_initializes_UI_fields_with_values_from_the_initial_configuration() {
    ExportConfiguration initialConfiguration = ExportConfiguration.empty();
    initialConfiguration.setExportDir(Paths.get("/some/path"));
    initialConfiguration.setPemFile(Paths.get("/some/file.pem"));
    initialConfiguration.setDateRangeStart(LocalDate.of(2018, 1, 1));
    initialConfiguration.setDateRangeEnd(LocalDate.of(2019, 1, 1));
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    new ConfigurationPanel(initialConfiguration, view);

    assertThat(view.getExportDir(), is(initialConfiguration.getExportDir().get()));
    assertThat(view.getPemFile(), is(initialConfiguration.getPemFile().get()));
    assertThat(view.getDateRangeStart(), is(initialConfiguration.getDateRangeStart().get()));
    assertThat(view.getDateRangeEnd(), is(initialConfiguration.getDateRangeEnd().get()));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_end_that_is_before_its_start() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    view.setDateRangeStart(LocalDate.of(2019, 1, 1));

    // Trigger the error
    view.setDateRangeEnd(LocalDate.of(2018, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.dateRangeEndField.getDate(), nullValue());
    assertThat(panel.getConfiguration().getDateRangeEnd(), is(Optional.empty()));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_start_that_is_after_its_end() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    view.setDateRangeEnd(LocalDate.of(2018, 1, 1));

    // Trigger the error
    view.setDateRangeStart(LocalDate.of(2019, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.dateRangeStartField.getDate(), nullValue());
    assertThat(configurationPanel.getConfiguration().getDateRangeStart(), is(Optional.empty()));
  }

  @Test
  public void it_can_be_disabled() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's enabled
    configurationPanel.enable();

    configurationPanel.disable();

    assertThat(view.enabled, is(false));
  }

  @Test
  public void it_can_be_enabled() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's disabled
    configurationPanel.disable();

    configurationPanel.enable();

    assertThat(view.enabled, is(true));
  }

  @Test
  public void knows_if_its_configuration_model_is_valid() throws IOException {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    // An empty configuration is not valid by default
    assertThat(configurationPanel.isValid(), is(false));

    // we make it valid by setting an existing export dir
    view.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(configurationPanel.isValid(), is(true));
  }

  @Test
  public void broadcasts_changes() {
    final AtomicInteger counter = new AtomicInteger(0);
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    panel.onChange(counter::incrementAndGet);

    view.setExportDir(Paths.get("/some/path"));
    view.setPemFile(Paths.get("/some/file.pem"));
    view.setDateRangeStart(LocalDate.of(2018, 1, 1));
    view.setDateRangeEnd(LocalDate.of(2019, 1, 1));

    assertThat(counter.get(), is(4));
  }

  static class TestConfigurationPanelView extends ConfigurationPanelView {
    private boolean errorShown = false;
    private boolean enabled;

    @Override
    public void showError(String message, String title) {
      errorShown = true;
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Path getExportDir() {
      return Paths.get(exportDirectoryField.getText());
    }

    public Path getPemFile() {
      return Paths.get(pemFileField.getText());
    }

    public LocalDate getDateRangeStart() {
      return LocalDate.of(
          dateRangeStartField.getDate().getYear(),
          dateRangeStartField.getDate().getMonthValue(),
          dateRangeStartField.getDate().getDayOfMonth()
      );
    }

    public LocalDate getDateRangeEnd() {
      return LocalDate.of(
          dateRangeEndField.getDate().getYear(),
          dateRangeEndField.getDate().getMonthValue(),
          dateRangeEndField.getDate().getDayOfMonth()
      );
    }
  }
}