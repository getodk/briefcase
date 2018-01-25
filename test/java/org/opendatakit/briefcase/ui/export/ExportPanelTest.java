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
package org.opendatakit.briefcase.ui.export;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.SwingTestRig.classPath;
import static org.opendatakit.briefcase.ui.SwingTestRig.createCache;
import static org.opendatakit.briefcase.ui.SwingTestRig.installFormsFrom;
import static org.opendatakit.briefcase.ui.SwingTestRig.prepareBriefcaseStorageFolder;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.enabled;

import java.io.IOException;
import java.nio.file.Files;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CacioFESTRunner.class)
public class ExportPanelTest extends AssertJSwingJUnitTestCase {
  private ExportPanelPageObject page;

  @BeforeClass
  public static void init() {
    prepareBriefcaseStorageFolder();
    installFormsFrom(classPath("/exportPanelTest/forms"));
    createCache();
  }

  @Override
  protected void onSetUp() {
    page = ExportPanelPageObject.setUp(robot());
    page.show();
  }

  @Test
  public void export_button_should_be_disabled_by_default() {
    assertThat(page.exportButton(), is(not(enabled())));
  }

  @Test
  public void export_button_should_be_enabled_when_there_are_no_configuration_errors() throws IOException {
    // Export dir must exist to be valid
    page.setExportDirectory(Files.createTempDirectory("test_briefcase_export").toString());
    page.selectFormATRow(0);
    assertThat(page.exportButton(), is(enabled()));
  }
}