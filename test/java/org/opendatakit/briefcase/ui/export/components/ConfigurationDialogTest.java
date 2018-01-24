package org.opendatakit.briefcase.ui.export.components;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.matchers.GenericUIMatchers.visible;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.enabled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationDialogTest extends AssertJSwingJUnitTestCase {
  private static ExportConfiguration CONFIGURATION;

  static {
    try {
      CONFIGURATION = ExportConfiguration.empty();
      CONFIGURATION.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private ConfigurationDialogPageObject page;

  @Override
  protected void onSetUp() {
  }

  @Test
  public void ok_button_is_enabled_with_an_empty_initial_configuration() {
    page = ConfigurationDialogPageObject.setUp(robot(), ExportConfiguration.empty());
    page.show();
    assertThat(page.okButton(), is(enabled()));
  }

  @Test
  public void ok_button_is_enabled_with_a_non_empty_initial_configuration() {
    page = ConfigurationDialogPageObject.setUp(robot(), CONFIGURATION);
    page.show();
    assertThat(page.okButton(), is(enabled()));
  }

  @Test
  public void clear_all_button_is_disabled_with_an_empty_initial_configuration() {
    page = ConfigurationDialogPageObject.setUp(robot(), ExportConfiguration.empty());
    page.show();
    assertThat(page.clearAllButton(), is(not(enabled())));
  }

  @Test
  public void clear_all_button_is_enabled_with_a_non_empty_initial_configuration() {
    page = ConfigurationDialogPageObject.setUp(robot(), CONFIGURATION);
    page.show();
    assertThat(page.clearAllButton(), is(enabled()));
  }

  @Test
  public void clear_all_button_is_enabled_when_the_configuration_is_not_empty() {
    page = ConfigurationDialogPageObject.setUp(robot(), ExportConfiguration.empty());
    page.show();
    page.setSomeExportDir();
    assertThat(page.clearAllButton(), is(enabled()));
    page.clearExportDir();
    assertThat(page.clearAllButton(), is(not(enabled())));
  }

  @Test
  public void ok_button_closes_the_dialog() {
    page = ConfigurationDialogPageObject.setUp(robot(), CONFIGURATION);
    page.show();
    page.clickOnOk();
    assertThat(page.dialog(), is(not(visible())));
  }

  @Test
  public void remove_button_closes_the_dialog() {
    page = ConfigurationDialogPageObject.setUp(robot(), CONFIGURATION);
    page.show();
    page.clickOnRemove();
    assertThat(page.dialog(), is(not(visible())));
  }

  @Test
  public void cancel_button_closes_the_dialog() {
    page = ConfigurationDialogPageObject.setUp(robot(), CONFIGURATION);
    page.show();
    page.clickOnCancel();
    assertThat(page.dialog(), is(not(visible())));
  }
}