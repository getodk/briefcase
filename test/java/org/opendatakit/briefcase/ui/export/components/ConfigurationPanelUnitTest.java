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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationPanelUnitTest {
  @Test
  public void it_wires_UI_fields_to_the_model() {
    ExportConfiguration expectedConfiguration = ExportConfiguration.empty();
    expectedConfiguration.setExportDir(Paths.get("/some/path"));
    expectedConfiguration.setPemFile(Paths.get("/some/file.pem"));
    expectedConfiguration.setStartDate(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setEndDate(LocalDate.of(2019, 1, 1));
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
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
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
    new ConfigurationPanel(initialConfiguration, view);

    assertThat(view.getExportDir(), is(initialConfiguration.getExportDir().get()));
    assertThat(view.getPemFile(), is(initialConfiguration.getPemFile().get()));
    assertThat(view.getDateRangeStart(), is(initialConfiguration.getStartDate().get()));
    assertThat(view.getDateRangeEnd(), is(initialConfiguration.getEndDate().get()));
  }

  @Test
  public void it_can_be_disabled() {
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's enabled
    configurationPanel.enable();

    configurationPanel.disable();

    assertThat(view.enabled, is(false));
  }

  @Test
  public void it_can_be_enabled() {
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
    ConfigurationPanel configurationPanel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    // ensure that it's disabled
    configurationPanel.disable();

    configurationPanel.enable();

    assertThat(view.enabled, is(true));
  }

  @Test
  public void knows_if_its_configuration_model_is_valid() throws IOException {
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
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
    FakeConfigurationPanelForm view = new FakeConfigurationPanelForm(false);
    ConfigurationPanel panel = new ConfigurationPanel(ExportConfiguration.empty(), view);
    panel.onChange(counter::incrementAndGet);

    view.setExportDir(Paths.get("/some/path"));
    view.setPemFile(Paths.get("/some/file.pem"));
    view.setStartDate(LocalDate.of(2018, 1, 1));
    view.setEndDate(LocalDate.of(2019, 1, 1));

    assertThat(counter.get(), is(4));
  }

}
