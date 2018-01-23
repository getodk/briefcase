package org.opendatakit.briefcase.ui.export.components;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.enabled;

import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationDialogWithInvalidConfTest extends AssertJSwingJUnitTestCase {
  private ConfigurationDialogPageObject dialog;

  @Override
  protected void onSetUp() {
    dialog = ConfigurationDialogPageObject.setUp(robot(), ExportConfiguration.empty());
    dialog.show();
  }

  @Test
  public void clearAll_button_is_disabled_with_an_invalid_initial_configuration() {
    assertThat(dialog.clearAllButton(), is(not(enabled())));
  }

}