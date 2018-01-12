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
    expectedConfiguration.setStartDate(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setEndDate(LocalDate.of(2019, 1, 1));
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    view.setExportDir(expectedConfiguration.getExportDir().get());
    view.setPemFile(expectedConfiguration.getPemFile().get());
    view.setStartDate(expectedConfiguration.getStartDate().get());
    view.setEndDate(expectedConfiguration.getEndDate().get());

    assertThat(panel.getConfiguration(), equalTo(expectedConfiguration));
  }

  @Test
  public void it_initializes_UI_fields_with_values_from_the_initial_configuration() {
    ExportConfiguration initialConfiguration = ExportConfiguration.empty();
    initialConfiguration.setExportDir(Paths.get("/some/path"));
    initialConfiguration.setPemFile(Paths.get("/some/file.pem"));
    initialConfiguration.setStartDate(LocalDate.of(2018, 1, 1));
    initialConfiguration.setEndDate(LocalDate.of(2019, 1, 1));
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    new ConfigurationPanel(initialConfiguration, view);

    assertThat(view.getExportDir(), is(initialConfiguration.getExportDir().get()));
    assertThat(view.getPemFile(), is(initialConfiguration.getPemFile().get()));
    assertThat(view.getDateRangeStart(), is(initialConfiguration.getStartDate().get()));
    assertThat(view.getDateRangeEnd(), is(initialConfiguration.getEndDate().get()));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_end_that_is_before_its_start() {
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    view.setStartDate(LocalDate.of(2019, 1, 1));

    // Trigger the error
    view.setEndDate(LocalDate.of(2018, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.endDateField.getDate(), nullValue());
    assertThat(panel.getConfiguration().getEndDate(), is(Optional.empty()));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_start_that_is_after_its_end() {
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    view.setEndDate(LocalDate.of(2018, 1, 1));

    // Trigger the error
    view.setStartDate(LocalDate.of(2019, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.startDateField.getDate(), nullValue());
    assertThat(configurationPanel.getConfiguration().getStartDate(), is(Optional.empty()));
  }

  @Test
  public void it_can_be_disabled() {
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's enabled
    configurationPanel.enable();

    configurationPanel.disable();

    assertThat(view.enabled, is(false));
  }

  @Test
  public void it_can_be_enabled() {
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's disabled
    configurationPanel.disable();

    configurationPanel.enable();

    assertThat(view.enabled, is(true));
  }

  @Test
  public void knows_if_its_configuration_model_is_valid() throws IOException {
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
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
    TestConfigurationPanelPanelView view = new TestConfigurationPanelPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    panel.onChange(counter::incrementAndGet);

    view.setExportDir(Paths.get("/some/path"));
    view.setPemFile(Paths.get("/some/file.pem"));
    view.setStartDate(LocalDate.of(2018, 1, 1));
    view.setEndDate(LocalDate.of(2019, 1, 1));

    assertThat(counter.get(), is(4));
  }

  static class TestConfigurationPanelPanelView extends ConfigurationPanelForm {
    private boolean errorShown = false;
    private boolean enabled;

//    @Override
//    public void showError(String message, String title) {
//      errorShown = true;
//    }

    @Override
    public void enable() {
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
          startDateField.getDate().getYear(),
          startDateField.getDate().getMonthValue(),
          startDateField.getDate().getDayOfMonth()
      );
    }

    public LocalDate getDateRangeEnd() {
      return LocalDate.of(
          endDateField.getDate().getYear(),
          endDateField.getDate().getMonthValue(),
          endDateField.getDate().getDayOfMonth()
      );
    }
  }
}