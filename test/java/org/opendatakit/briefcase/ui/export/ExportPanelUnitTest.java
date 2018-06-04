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
import org.opendatakit.briefcase.export.ExportEvent;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusBuilder;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.ui.reused.NoOpAnalytics;

public class ExportPanelUnitTest {

  private ExportConfiguration initialDefaultConf;

  @Test
  public void saves_to_user_preferences_changes_on_the_default_configuration() throws IOException {
    BriefcasePreferences exportPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences appPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    initialDefaultConf = ExportConfiguration.empty();
    ExportForms forms = load(initialDefaultConf, new ArrayList<>(), exportPreferences, appPreferences);
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(initialDefaultConf, false, true);
    new ExportPanel(
        forms,
        ExportPanelForm.from(forms, confPanel),
        appPreferences,
        exportPreferences,
        new NoOpAnalytics()
    );

    assertThat(ExportConfiguration.load(exportPreferences).getExportDir(), isEmpty());
    confPanel.getForm().setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(ExportConfiguration.load(exportPreferences).getExportDir(), isPresent());
  }

  @Test
  public void saves_to_user_preferences_changes_on_a_custom_configuration() throws IOException {
    BriefcasePreferences exportPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences appPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    List<FormStatus> formsList = FormStatusBuilder.buildFormStatusList(10);
    initialDefaultConf = ExportConfiguration.empty();
    ExportForms forms = load(initialDefaultConf, formsList, exportPreferences, appPreferences);
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(initialDefaultConf, true, true);
    ExportPanelForm exportPanelForm = ExportPanelForm.from(forms, confPanel);
    new ExportPanel(
        forms,
        exportPanelForm,
        appPreferences,
        exportPreferences,
        new NoOpAnalytics()
    );

    FormStatus form = formsList.get(0);
    String formId = form.getFormDefinition().getFormId();

    ExportConfiguration conf = ExportConfiguration.empty();
    conf.setExportDir(Paths.get(Files.createTempDirectory("briefcase_test").toUri()));

    assertThat(ExportConfiguration.load(exportPreferences, buildCustomConfPrefix(formId)).getExportDir(), isEmpty());

    forms.putConfiguration(form, conf);
    exportPanelForm.getFormsTable().getViewModel().triggerChange();

    assertThat(ExportConfiguration.load(exportPreferences, buildCustomConfPrefix(formId)).getExportDir(), isPresent());
  }

  @Test
  public void saves_to_user_preferences_the_last_successful_export_date_for_a_form() {
    BriefcasePreferences exportPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences appPreferences = new BriefcasePreferences(InMemoryPreferences.empty());
    List<FormStatus> formsList = FormStatusBuilder.buildFormStatusList(10);
    initialDefaultConf = ExportConfiguration.empty();
    ExportForms forms = load(initialDefaultConf, formsList, exportPreferences, appPreferences);
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(initialDefaultConf, true, true);
    new ExportPanel(
        forms,
        ExportPanelForm.from(forms, confPanel),
        appPreferences,
        exportPreferences,
        new NoOpAnalytics()
    );

    FormStatus form = formsList.get(0);
    String formId = form.getFormDefinition().getFormId();

    assertThat(exportPreferences.nullSafeGet(buildExportDateTimePrefix(formId)), isEmpty());

    FormDefinition formDef = FormDefinition.from((BriefcaseFormDefinition) form.getFormDefinition());
    ExportEvent event = ExportEvent.successForm(formDef, 10);
    forms.appendStatus(event);

    assertThat(exportPreferences.nullSafeGet(buildExportDateTimePrefix(formId)), isPresent());
  }
}