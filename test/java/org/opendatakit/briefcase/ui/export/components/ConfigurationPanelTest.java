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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.GenericUIMatchers.containsText;
import static org.opendatakit.briefcase.matchers.SwingMatchers.enabled;
import static org.opendatakit.briefcase.matchers.SwingMatchers.selected;
import static org.opendatakit.briefcase.matchers.SwingMatchers.visible;
import static org.opendatakit.briefcase.ui.export.components.CustomConfBoolean.Value.YES;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.matchers.GenericUIMatchers;
import org.opendatakit.briefcase.matchers.SwingMatchers;

public class ConfigurationPanelTest extends AssertJSwingJUnitTestCase {
  private ConfigurationPanelPageObject component;

  @Override
  protected void onSetUp() {
    // component creation is made on each test to allow different scenarios
  }

  @Test
  @Ignore
  public void export_dir_button_opens_a_file_dialog() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.clickChooseExportDirButton();
    assertThat(component.fileDialog(2000), is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  @Ignore
  public void pem_file_button_opens_a_file_dialog() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.clickChoosePemFileButton();
    assertThat(component.fileDialog(2000), is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  public void only_the_pem_file_choose_button_is_visible_initially() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    assertThat(component.choosePemFileButton(), is(visible()));
    assertThat(component.clearPemFileButton(), is(not(visible())));
  }

  @Test
  public void choose_pem_file_button_is_swapped_for_a_clean_button_that_cleans_the_field() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.setSomePemFile();
    assertThat(component.choosePemFileButton(), is(not(visible())));
    assertThat(component.clearPemFileButton(), is(visible()));
    component.clickClearPemFileButton();
    assertThat(component.pemFileField(), is(SwingMatchers.empty()));
  }

  @Test
  public void cannot_insert_an_end_date_before_the_set_start_date() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.setStartDate(LocalDate.of(2017, 1, 30));
    assertFalse(component.endDateField().isDateAllowed(LocalDate.of(2017, 1, 28)));
  }

  @Test
  public void cannot_insert_a_start_date_after_the_set_end_date() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.setEndDate(LocalDate.of(2017, 1, 25));
    assertFalse(component.startDateField().isDateAllowed(LocalDate.of(2017, 1, 27)));
  }

  @Test
  public void default_panel_loads_values_from_the_initial_configuration() {
    ExportConfiguration expectedConfiguration = ExportConfiguration.empty();
    expectedConfiguration.setExportDir(Paths.get("/some/path"));
    expectedConfiguration.setPemFile(Paths.get("/some/file.pem"));
    expectedConfiguration.setStartDate(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setEndDate(LocalDate.of(2019, 1, 1));
    expectedConfiguration.setPullBefore(true);
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), expectedConfiguration, true, true);
    component.show();
    assertThat(component.exportDirField(), containsText(expectedConfiguration.getExportDir().get().toString()));
    assertThat(component.pemFileField(), containsText(expectedConfiguration.getPemFile().get().toString()));
    assertThat(component.startDateField().getDate(), is(expectedConfiguration.getStartDate().get()));
    assertThat(component.endDateField().getDate(), is(expectedConfiguration.getEndDate().get()));
    assertThat(component.pullBeforeField(), is(selected()));
  }

  @Test
  public void default_panel_can_be_disabled() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.disable(true);
    assertThat(component.exportDirField(), is(not(enabled())));
    assertThat(component.pemFileField(), is(not(enabled())));
    assertThat(component.startDateField(), is(not(enabled())));
    assertThat(component.endDateField(), is(not(enabled())));
    assertThat(component.pullBeforeField(), is(not(enabled())));
  }

  @Test
  public void default_panel_can_be_enabled() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.disable(true);
    component.enable(true);
    assertThat(component.exportDirField(), is(enabled()));
    assertThat(component.pemFileField(), is(enabled()));
    assertThat(component.startDateField(), is(enabled()));
    assertThat(component.endDateField(), is(enabled()));
    assertThat(component.pullBeforeField(), is(enabled()));
  }

  @Test
  public void default_panel_wires_UI_fields_to_the_model() {
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();

    component.setSomePemFile();
    component.setSomeExportDir();
    component.setSomeStartDate();
    component.setSomeEndDate();
    component.setPullBefore(true);
    ExportConfiguration conf = component.getConfiguration();
    assertThat(conf.getExportDir(), isPresent());
    assertThat(conf.getPemFile(), isPresent());
    assertThat(conf.getStartDate(), isPresent());
    assertThat(conf.getEndDate(), isPresent());
    assertThat(conf.getPullBefore(), isPresent());
  }

  @Test
  public void default_panel_broadcasts_changes() {
    final AtomicInteger counter = new AtomicInteger(0);
    component = ConfigurationPanelPageObject.setUpDefaultPanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.onChange(counter::incrementAndGet);

    component.setSomePemFile();
    component.setSomeExportDir();
    component.setSomeStartDate();
    component.setSomeEndDate();
    component.setPullBefore(true);

    assertThat(counter.get(), CoreMatchers.is(5));
  }

  @Test
  public void override_panel_loads_values_from_the_initial_configuration() {
    ExportConfiguration expectedConfiguration = ExportConfiguration.empty();
    expectedConfiguration.setExportDir(Paths.get("/some/path"));
    expectedConfiguration.setPemFile(Paths.get("/some/file.pem"));
    expectedConfiguration.setStartDate(LocalDate.of(2018, 1, 1));
    expectedConfiguration.setEndDate(LocalDate.of(2019, 1, 1));
    expectedConfiguration.setPullBeforeOverride(YES);
    component = ConfigurationPanelPageObject.setUpOverridePanel(robot(), expectedConfiguration, true, true);
    component.show();
    assertThat(component.exportDirField(), containsText(expectedConfiguration.getExportDir().get().toString()));
    assertThat(component.pemFileField(), containsText(expectedConfiguration.getPemFile().get().toString()));
    assertThat(component.startDateField().getDate(), is(expectedConfiguration.getStartDate().get()));
    assertThat(component.endDateField().getDate(), is(expectedConfiguration.getEndDate().get()));
    assertThat(component.pullBeforeOverrideField().get(), is(YES));
  }

  @Test
  public void override_panel_can_be_disabled() {
    component = ConfigurationPanelPageObject.setUpOverridePanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.disable(true);
    assertThat(component.exportDirField(), is(not(enabled())));
    assertThat(component.pemFileField(), is(not(enabled())));
    assertThat(component.startDateField(), is(not(enabled())));
    assertThat(component.endDateField(), is(not(enabled())));
    assertThat(component.pullBeforeOverrideLabel(), is(not(enabled())));
    assertThat(component.pullBeforeOverrideField().$$$getRootComponent$$$(), is(not(enabled())));
    assertThat(component.pullBeforeHintPanel(), is(not(enabled())));
  }

  @Test
  public void override_panel__can_be_enabled() {
    component = ConfigurationPanelPageObject.setUpOverridePanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.disable(true);
    component.enable(true);
    assertThat(component.exportDirField(), is(enabled()));
    assertThat(component.pemFileField(), is(enabled()));
    assertThat(component.startDateField(), is(enabled()));
    assertThat(component.endDateField(), is(enabled()));
    assertThat(component.pullBeforeOverrideLabel(), is(enabled()));
    assertThat(component.pullBeforeOverrideField().$$$getRootComponent$$$(), is(enabled()));
    assertThat(component.pullBeforeHintPanel(), is(enabled()));
  }

  @Test
  public void override_panel_wires_UI_fields_to_the_model() {
    component = ConfigurationPanelPageObject.setUpOverridePanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();

    component.setSomePemFile();
    component.setSomeExportDir();
    component.setSomeStartDate();
    component.setSomeEndDate();
    component.setPullBeforeOverride(YES);
    ExportConfiguration conf = component.getConfiguration();
    assertThat(conf.getExportDir(), isPresent());
    assertThat(conf.getPemFile(), isPresent());
    assertThat(conf.getStartDate(), isPresent());
    assertThat(conf.getEndDate(), isPresent());
    assertThat(conf.getPullBeforeOverride(), isPresent());
  }

  @Test
  public void override_panel_broadcasts_changes() {
    final AtomicInteger counter = new AtomicInteger(0);
    component = ConfigurationPanelPageObject.setUpOverridePanel(robot(), ExportConfiguration.empty(), true, true);
    component.show();
    component.onChange(counter::incrementAndGet);

    component.setSomePemFile();
    component.setSomeExportDir();
    component.setSomeStartDate();
    component.setSomeEndDate();
    component.setPullBeforeOverride(YES);

    // Note that there are 6 calls instead of 5 because the panel
    // will implicitly set it to the default INHERIT option on creation
    assertThat(counter.get(), is(6));
  }
}
