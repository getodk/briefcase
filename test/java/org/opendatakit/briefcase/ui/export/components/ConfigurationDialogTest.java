package org.opendatakit.briefcase.ui.export.components;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.matchers.GenericUIMatchers.visible;

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