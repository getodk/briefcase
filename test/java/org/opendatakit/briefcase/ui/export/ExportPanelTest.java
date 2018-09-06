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
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.SwingMatchers.enabled;

import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

public class ExportPanelTest extends AssertJSwingJUnitTestCase {
  private ExportPanelPageObject page;

  @Override
  protected void onSetUp() {
    page = ExportPanelPageObject.setUp(robot());
    page.show();
  }

  @Test
  public void export_button_should_be_enabled_by_default() {
    assertThat(page.exportButton(), is(enabled()));
  }

}