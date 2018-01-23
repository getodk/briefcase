package org.opendatakit.briefcase.export;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.ExportForms.buildCustomConfPrefix;
import static org.opendatakit.briefcase.model.FormStatusBuilder.buildFormStatusList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.InMemoryPreferences;

public class ExportFormsTest {
  private static ExportConfiguration VALID_CONFIGURATION;
  private static ExportConfiguration INVALID_CONFIGURATION;

  static {
    try {
      VALID_CONFIGURATION = ExportConfiguration.empty();
      VALID_CONFIGURATION.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));
      INVALID_CONFIGURATION = ExportConfiguration.empty();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void can_merge_an_incoming_list_of_forms() {
    ExportForms forms = new ExportForms(new ArrayList<>(), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    assertThat(forms.size(), is(0));
    int expectedSize = 10;

    forms.merge(buildFormStatusList(expectedSize));

    assertThat(forms.size(), is(expectedSize));
  }

  @Test
  public void manages_a_forms_configuration() {
    ExportForms forms = new ExportForms(buildFormStatusList(2), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    FormStatus firstForm = forms.get(0);
    FormStatus secondForm = forms.get(1);

    assertThat(forms.hasConfiguration(firstForm), is(false));

    forms.putConfiguration(firstForm, VALID_CONFIGURATION);

    assertThat(forms.hasConfiguration(firstForm), is(true));
    assertThat(forms.getConfiguration(firstForm.getFormDefinition().getFormId()), is(VALID_CONFIGURATION));
    assertThat(forms.getConfiguration(firstForm.getFormDefinition().getFormId()), is(VALID_CONFIGURATION));

    forms.putConfiguration(secondForm, INVALID_CONFIGURATION);
    assertThat(forms.getCustomConfigurations().values(), hasSize(2));
    assertThat(forms.getCustomConfigurations().values(), contains(VALID_CONFIGURATION, INVALID_CONFIGURATION));

    forms.removeConfiguration(firstForm);

    assertThat(forms.hasConfiguration(firstForm), is(false));
  }

  @Test
  public void manages_forms_selection() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));

    forms.get(0).setSelected(true);
    assertThat(forms.getSelectedForms(), hasSize(1));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.selectAll();
    assertThat(forms.getSelectedForms(), hasSize(10));
    assertThat(forms.allSelected(), is(true));
    assertThat(forms.noneSelected(), is(false));
    assertThat(forms.someSelected(), is(true));

    forms.clearAll();
    assertThat(forms.getSelectedForms(), hasSize(0));
    assertThat(forms.allSelected(), is(false));
    assertThat(forms.noneSelected(), is(true));
    assertThat(forms.someSelected(), is(false));
  }

  @Test
  public void knows_if_all_selected_forms_have_a_valid_configuration() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    FormStatus form = forms.get(0);
    form.setSelected(true);

    assertThat(forms.allSelectedFormsHaveConfiguration(), is(false));

    forms.putConfiguration(form, VALID_CONFIGURATION);

    assertThat(forms.allSelectedFormsHaveConfiguration(), is(true));
  }

  @Test
  public void appends_status_history_on_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    FormStatus form = forms.get(0);
    forms.appendStatus(form.getFormDefinition(), "some status update", false);
    forms.appendStatus(form.getFormDefinition(), "some more lines", false);

    assertThat(form.getStatusHistory(), containsString("some status update"));
    assertThat(form.getStatusHistory(), containsString("some more lines"));
    assertThat(form.getStatusHistory().split("\n").length, is(3)); // There is a leading \n
  }

  @Test
  public void when_there_is_a_status_history_update_thats_been_successful_it_registers_an_export_date() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    FormStatus form = forms.get(0);

    assertThat(forms.getLastExportDateTime(form), isEmpty());

    forms.appendStatus(form.getFormDefinition(), "some status update", true);

    assertThat(forms.getLastExportDateTime(form), isPresent());
  }

  @Test
  public void it_lets_a_third_party_react_to_successful_exports() {
    AtomicInteger count = new AtomicInteger(0);
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    forms.onSuccessfulExport((form, exportDateTime) -> {
      if (form.equals(forms.get(0).getFormDefinition().getFormId()))
        count.incrementAndGet();
    });

    forms.appendStatus(forms.get(0).getFormDefinition(), "some status update", true);

    assertThat(count.get(), is(1));
  }

  @Test
  public void it_has_a_factory_that_creates_a_new_instance_from_saved_preferences() {
    LocalDateTime exportDateTime = LocalDateTime.now();
    List<FormStatus> formsList = buildFormStatusList(10);
    FormStatus form = formsList.get(0);
    String formId = form.getFormDefinition().getFormId();
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(VALID_CONFIGURATION.asMap(buildCustomConfPrefix(formId)));
    prefs.put(ExportForms.buildExportDateTimePrefix(formId), exportDateTime.format(ISO_DATE_TIME));

    ExportForms forms = ExportForms.load(ExportConfiguration.empty(), formsList, prefs);

    assertThat(forms.size(), is(10));
    assertThat(forms.hasConfiguration(form), is(true));
    assertThat(forms.getLastExportDateTime(form), isPresent());
  }
}