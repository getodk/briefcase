package org.opendatakit.briefcase.ui.export;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.ExportForms.buildCustomConfPrefix;
import static org.opendatakit.briefcase.export.ExportForms.buildExportDateTimePrefix;
import static org.opendatakit.briefcase.export.ExportForms.load;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusBuilder;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;

public class ExportPanelUnitTest {

  private ExportConfiguration initialDefaultConf;

  @Test
  public void saves_to_user_preferences_changes_on_the_default_configuration() throws IOException {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    ExportForms forms = load(new ArrayList<>(), prefs);
    initialDefaultConf = ExportConfiguration.empty();
    ConfigurationPanel confPanel = ConfigurationPanel.from(initialDefaultConf);
    new ExportPanel(
      new TerminationFuture(),
        forms,
        ExportPanelForm.from(forms, confPanel),
        prefs
    );

    assertThat(ExportConfiguration.load(prefs).getExportDir(), isEmpty());
    confPanel.getForm().setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(ExportConfiguration.load(prefs).getExportDir(), isPresent());
  }

  @Test
  public void saves_to_user_preferences_changes_on_a_custom_configuration() throws IOException {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    List<FormStatus> formsList = FormStatusBuilder.buildFormStatusList(10);
    ExportForms forms = load(formsList, prefs);
    initialDefaultConf = ExportConfiguration.empty();
    ConfigurationPanel confPanel = ConfigurationPanel.from(initialDefaultConf);
    ExportPanelForm exportPanelForm = ExportPanelForm.from(forms, confPanel);
    new ExportPanel(
        new TerminationFuture(),
        forms,
        exportPanelForm,
        prefs
    );

    FormStatus form = formsList.get(0);
    String formId = form.getFormDefinition().getFormId();

    ExportConfiguration conf = ExportConfiguration.empty();
    conf.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(ExportConfiguration.load(prefs, buildCustomConfPrefix(formId)).getExportDir(), isEmpty());

    forms.setConfiguration(form, conf);
    exportPanelForm.getFormsTable().getViewModel().triggerChange();

    assertThat(ExportConfiguration.load(prefs, buildCustomConfPrefix(formId)).getExportDir(), isPresent());
  }

  @Test
  public void saves_to_user_preferences_the_last_successful_export_date_for_a_form() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    List<FormStatus> formsList = FormStatusBuilder.buildFormStatusList(10);
    ExportForms forms = load(formsList, prefs);
    initialDefaultConf = ExportConfiguration.empty();
    ConfigurationPanel confPanel = ConfigurationPanel.from(initialDefaultConf);
    new ExportPanel(
        new TerminationFuture(),
        forms,
        ExportPanelForm.from(forms, confPanel),
        prefs
    );

    FormStatus form = formsList.get(0);
    String formId = form.getFormDefinition().getFormId();

    assertThat(prefs.nullSafeGet(buildExportDateTimePrefix(formId)), isEmpty());

    forms.appendStatus(form.getFormDefinition(), "some status update", true);

    assertThat(prefs.nullSafeGet(buildExportDateTimePrefix(formId)), isPresent());
  }
}