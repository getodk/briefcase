package org.opendatakit.briefcase.ui.export.components;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;
import org.opendatakit.briefcase.ui.export.ExportConfiguration;

public class ConfigurationPanelTest {
  @Test
  public void the_component_wires_UI_fields_to_the_model() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    ExportConfiguration expectedConfiguration = ExportConfiguration.empty();
    expectedConfiguration.setExportDir(Paths.get("/some/path"));
    expectedConfiguration.setPemFile(Paths.get("/some/file.pem"));
    expectedConfiguration.setDateRangeStart(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setDateRangeEnd(LocalDate.of(2019, 1, 1));

    view.setExportDir(Paths.get("/some/path"));
    view.setPemFile(Paths.get("/some/file.pem"));
    view.setDateRangeStart(LocalDate.of(2018, 1, 1));
    view.setDateRangeEnd(LocalDate.of(2019, 1, 1));

    assertThat(panel.getConfiguration(), equalTo(expectedConfiguration));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_end_is_before_its_start() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    view.setDateRangeStart(LocalDate.of(2019, 1, 1));
    view.setDateRangeEnd(LocalDate.of(2018, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.dateRangeEndField.getDate(), nullValue());
    assertThat(configurationPanel.getConfiguration().getDateRangeEnd(), is(Optional.empty()));
  }

  @Test
  public void shows_an_error_and_resets_the_field_when_the_UI_tries_to_set_a_date_range_start_is_after_its_end() {
    TestConfigurationPanelView view = new TestConfigurationPanelView();
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);

    view.setDateRangeEnd(LocalDate.of(2018, 1, 1));
    view.setDateRangeStart(LocalDate.of(2019, 1, 1));

    assertThat(view.errorShown, is(true));
    assertThat(view.dateRangeStartField.getDate(), nullValue());
    assertThat(configurationPanel.getConfiguration().getDateRangeStart(), is(Optional.empty()));
  }

  static class TestConfigurationPanelView extends ConfigurationPanelView {
    private boolean errorShown = false;
    private String errorMessage = "";
    private String errorTitle = "";

    @Override
    public void showError(String message, String title) {
      errorShown = true;
      errorMessage = message;
      errorTitle = title;
    }
  }
}