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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.empty;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.visible;

import java.time.LocalDate;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.opendatakit.briefcase.ui.matchers.GenericUIMatchers;

public class ConfigurationPanelTest extends AssertJSwingJUnitTestCase {
  private ConfigurationPanelPageObject component;

  @Override
  protected void onSetUp() {
    component = ConfigurationPanelPageObject.setUp(robot());
    component.show();
  }

  @Test
  @Ignore
  public void export_dir_button_opens_a_file_dialog() {
    component.clickChooseExportDirButton();
    assertThat(component.fileDialog(2000), is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  @Ignore
  public void pem_file_button_opens_a_file_dialog() {
    component.clickChoosePemFileButton();
    assertThat(component.fileDialog(2000), is(GenericUIMatchers.visible()));
    component.cancelFileDialog();
  }

  @Test
  public void only_the_pem_file_choose_button_is_visible_initially() {
    assertThat(component.choosePemFileButton(), is(visible()));
    assertThat(component.clearPemFileButton(), is(not(visible())));
  }

  @Test
  public void choose_pem_file_button_is_swapped_for_a_clean_button_that_cleans_the_field() {
    component.setSomePemFile();
    assertThat(component.choosePemFileButton(), is(not(visible())));
    assertThat(component.clearPemFileButton(), is(visible()));
    component.clickClearPemFileButton();
    assertThat(component.pemFileField(), is(empty()));
  }

  @Test
  public void cannot_insert_an_end_date_before_the_set_start_date() {
    component.setStartDate(LocalDate.of(2017, 1, 30));
    assertFalse(component.endDatePicker().isDateAllowed(LocalDate.of(2017, 1, 28)));
  }

  @Test
  public void cannot_insert_a_start_date_after_the_set_end_date() {
    component.setEndDate(LocalDate.of(2017,1,25));
    assertFalse(component.startDatePicker().isDateAllowed(LocalDate.of(2017,1,27)));
  }
}
