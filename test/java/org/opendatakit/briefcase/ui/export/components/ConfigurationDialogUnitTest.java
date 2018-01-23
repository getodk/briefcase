package org.opendatakit.briefcase.ui.export.components;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationDialogUnitTest {

  @Test
  public void the_ok_and_remove_buttons_are_disabled_by_default() {
    FakeConfigurationPanelForm confPanelForm = new FakeConfigurationPanelForm(false);

    FakeConfigurationDialogForm dialogForm = new FakeConfigurationDialogForm(confPanelForm);

    new ConfigurationDialog(dialogForm, new ConfigurationPanel(ExportConfiguration.empty(), confPanelForm));

    assertThat(dialogForm.okEnabled, is(false));
    // Kind of cheating because we set that to false in our Fake class
    assertThat(dialogForm.removeEnabled, is(false));
  }

  @Test
  public void the_dialog_enables_the_remove_button_if_it_receives_a_non_empty_initial_configuration() throws IOException {
    FakeConfigurationPanelForm confPanelForm = new FakeConfigurationPanelForm(false);

    FakeConfigurationDialogForm dialogForm = new FakeConfigurationDialogForm(confPanelForm);

    ExportConfiguration configuration = ExportConfiguration.empty();
    configuration.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));
    new ConfigurationDialog(dialogForm, new ConfigurationPanel(configuration, confPanelForm));

    assertThat(dialogForm.removeEnabled, is(true));
  }

  @Test
  public void the_dialog_enables_the_ok_button_when_the_user_sets_a_valid_configuration() throws IOException {
    FakeConfigurationPanelForm confPanelForm = new FakeConfigurationPanelForm(false);

    FakeConfigurationDialogForm dialogForm = new FakeConfigurationDialogForm(confPanelForm);

    new ConfigurationDialog(dialogForm, new ConfigurationPanel(ExportConfiguration.empty(), confPanelForm));

    assertThat(dialogForm.okEnabled, is(false));

    confPanelForm.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(dialogForm.okEnabled, is(true));
  }

  @Test
  public void the_dialog_disables_the_ok_button_if_the_configuration_stops_being_valid() throws IOException {
    FakeConfigurationPanelForm confPanelForm = new FakeConfigurationPanelForm(false);

    FakeConfigurationDialogForm dialogForm = new FakeConfigurationDialogForm(confPanelForm);

    ExportConfiguration configuration = ExportConfiguration.empty();
    configuration.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));
    new ConfigurationDialog(dialogForm, new ConfigurationPanel(configuration, confPanelForm));

    confPanelForm.setStartDate(LocalDate.now());
    // we leave the end date empty to make the configuration invalid

    assertThat(dialogForm.okEnabled, is(false));
  }

  @Test
  public void it_lets_third_parties_react_to_ok() throws IOException {
    FakeConfigurationPanelForm confPanelForm = new FakeConfigurationPanelForm(false);

    FakeConfigurationDialogForm dialogForm = new FakeConfigurationDialogForm(confPanelForm);

    ExportConfiguration configuration = ExportConfiguration.empty();
    ConfigurationDialog dialog = new ConfigurationDialog(dialogForm, new ConfigurationPanel(configuration, confPanelForm));
    AtomicBoolean okClicked = new AtomicBoolean(false);
    AtomicBoolean removeClicked = new AtomicBoolean(false);
    dialog.onOK(actualConf -> {
      okClicked.set(true);
      assertThat(actualConf.isValid(), is(true));
    });
    dialog.onRemove(() -> removeClicked.set(true));

    confPanelForm.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    dialogForm.clickOK();
    dialogForm.clickRemove();

    assertThat(okClicked.get(), is(true));
    assertThat(removeClicked.get(), is(true));
  }

}