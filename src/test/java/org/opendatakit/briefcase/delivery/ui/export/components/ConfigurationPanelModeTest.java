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
package org.opendatakit.briefcase.delivery.ui.export.components;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.opendatakit.briefcase.delivery.ui.export.components.ConfigurationPanelMode.REQUIRE_PULL_TEXT;
import static org.opendatakit.briefcase.delivery.ui.export.components.ConfigurationPanelMode.REQUIRE_SAVE_PASSWORDS;
import static org.opendatakit.briefcase.matchers.GenericUIMatchers.containsText;
import static org.opendatakit.briefcase.matchers.SwingMatchers.enabled;
import static org.opendatakit.briefcase.matchers.SwingMatchers.visible;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;

import java.util.Arrays;
import java.util.Collection;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class ConfigurationPanelModeTest extends AssertJSwingJUnitTestCase {
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public Scenario scenario;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Default - No save password", Scenario.defaultPanel(false, false).defaultField(true, false).hint(true, REQUIRE_SAVE_PASSWORDS)},
        {"Default - No save password (redundant)", Scenario.defaultPanel(false, true).defaultField(true, false).hint(true, REQUIRE_SAVE_PASSWORDS)},
        {"Default - No transfer settings", Scenario.defaultPanel(true, false).defaultField(true, true).hint(true, REQUIRE_PULL_TEXT)},
        {"Default - All requirements met", Scenario.defaultPanel(true, true).defaultField(true, true).hint(true, REQUIRE_PULL_TEXT)},
        {"Override - No save password", Scenario.overridePanel(false, false).overrideField(true, false).hint(true, REQUIRE_SAVE_PASSWORDS)},
        {"Override - No save password (redundant)", Scenario.overridePanel(false, true).overrideField(true, false).hint(true, REQUIRE_SAVE_PASSWORDS)},
        {"Override - No transfer settings", Scenario.overridePanel(true, false).overrideField(true, false).hint(true, REQUIRE_PULL_TEXT)},
        {"Override - All requirements met", Scenario.overridePanel(true, true).overrideField(true, true).hint(false, "")},
    });
  }

  @Override
  protected void onSetUp() {
    // component creation is made on each test to allow different scenarios
  }

  @Test
  public void decorates_fields_related_to_pulling_before_exporting() {
    ConfigurationPanelPageObject component = ConfigurationPanelPageObject.setUp(robot(), empty().build(), scenario.isOverridePanel, scenario.hasTransferSettings, scenario.savePasswords);
    component.show();

    // Pull Before checkbox assertions
    assertThat(component.pullBeforeField(), scenario.defaultFieldVisible ? is(visible()) : is(not(visible())));
    if (scenario.defaultFieldVisible)
      assertThat(component.pullBeforeField(), scenario.defaultFieldEnabled ? is(enabled()) : is(not(enabled())));

    // Pull Before Override label & combo box assertions
    assertThat(component.pullBeforeOverrideLabel(), scenario.overrideFieldVisible ? is(visible()) : is(not(visible())));
    assertThat(component.pullBeforeOverrideField().getContainer(), scenario.overrideFieldVisible ? is(visible()) : is(not(visible())));
    if (scenario.overrideFieldVisible)
      assertThat(component.pullBeforeOverrideField().getContainer(), scenario.overrideFieldEnabled ? is(enabled()) : is(not(enabled())));

    // Hint text pane assertions
    assertThat(component.pullBeforeHintPanel(), scenario.hintVisible ? is(visible()) : is(not(visible())));
    if (scenario.hintVisible)
      assertThat(component.pullBeforeHintPanel(), containsText(scenario.hintText));
  }

  static class Scenario {
    private final boolean isOverridePanel;
    private final boolean savePasswords;
    private final boolean hasTransferSettings;
    private boolean defaultFieldVisible;
    private boolean defaultFieldEnabled;
    private boolean overrideFieldVisible;
    private boolean overrideFieldEnabled;
    private boolean hintVisible;
    private String hintText;

    Scenario(boolean isOverridePanel, boolean savePasswords, boolean hasTransferSettings) {
      this.isOverridePanel = isOverridePanel;
      this.savePasswords = savePasswords;
      this.hasTransferSettings = hasTransferSettings;
    }

    static Scenario overridePanel(boolean savePasswords, boolean hasTransferSettings) {
      return new Scenario(true, savePasswords, hasTransferSettings)
          .defaultField(false, false);
    }

    static Scenario defaultPanel(boolean savePasswords, boolean hasTransferSettings) {
      return new Scenario(false, savePasswords, hasTransferSettings)
          .overrideField(false, false);
    }

    Scenario defaultField(boolean visible, boolean enabled) {
      this.defaultFieldVisible = visible;
      this.defaultFieldEnabled = enabled;
      return this;
    }

    Scenario overrideField(boolean visible, boolean enabled) {
      this.overrideFieldVisible = visible;
      this.overrideFieldEnabled = enabled;
      return this;
    }

    Scenario hint(boolean visible, String text) {
      this.hintVisible = visible;
      this.hintText = text;
      return this;
    }
  }
}
