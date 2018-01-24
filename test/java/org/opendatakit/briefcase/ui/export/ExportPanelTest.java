package org.opendatakit.briefcase.ui.export;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.SwingTestRig.classPath;
import static org.opendatakit.briefcase.ui.SwingTestRig.createInMemoryCache;
import static org.opendatakit.briefcase.ui.SwingTestRig.installFormsFrom;
import static org.opendatakit.briefcase.ui.matchers.SwingMatchers.enabled;

import java.io.IOException;
import java.nio.file.Files;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExportPanelTest extends AssertJSwingJUnitTestCase {
  private ExportPanelPageObject page;

  @BeforeClass
  public static void init() {
    createInMemoryCache();
    installFormsFrom(classPath("/exportPanelTest/forms"));
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