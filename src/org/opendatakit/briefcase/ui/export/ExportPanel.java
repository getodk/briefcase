/*
 * Copyright (C) 2011 University of Washington.
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

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.export.ExportForms.buildCustomConfPrefix;

import java.time.LocalDateTime;
import java.util.List;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class ExportPanel {
  public static final String TAB_NAME = "Export";

  private final TerminationFuture terminationFuture;
  private final ExportForms forms;
  private final ExportPanelForm form;

  public ExportPanel(TerminationFuture terminationFuture, BriefcasePreferences preferences) {
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    ExportConfiguration defaultConfiguration = ExportConfiguration.load(preferences);
    ConfigurationPanel confPanel = ConfigurationPanel.from(defaultConfiguration, false);

    forms = ExportForms.load(defaultConfiguration, getFormsFromStorage(), preferences);

    confPanel.onChange(() -> {
      forms.updateDefaultConfiguration(confPanel.getConfiguration());
    });

    forms.onSuccessfulExport((String formId, LocalDateTime exportDateTime) ->
        preferences.put(ExportForms.buildExportDateTimePrefix(formId), exportDateTime.format(ISO_DATE_TIME))
    );

    form = ExportPanelForm.from(forms, confPanel);

    form.onChange(() -> {
      // Clean all default conf keys
      preferences.removeAll(ExportConfiguration.keys());

      // Put default conf
      if (confPanel.isValid())
        preferences.putAll(confPanel.getConfiguration().asMap());

      // Clean all custom conf keys
      forms.forEach(formId ->
          preferences.removeAll(ExportConfiguration.keys(buildCustomConfPrefix(formId)))
      );

      // Put custom confs
      forms.getCustomConfigurations().forEach((formId, configuration) ->
          preferences.putAll(configuration.asMap(buildCustomConfPrefix(formId)))
      );

      if (forms.someSelected() && (confPanel.isValid() || forms.allSelectedFormsHaveConfiguration()))
        form.enableExport();
      else
        form.disableExport();

      if (forms.allSelected()) {
        form.toggleClearAll();
      } else {
        form.toggleSelectAll();
      }
    });


    form.onExport(() -> new Thread(() -> {
      List<String> errors = export();
      if (!errors.isEmpty()) {
        String message = String.format(
            "%s\n\n%s", "We have found some errors while performing the requested export actions:",
            errors.stream().map(e -> "- " + e).collect(joining("\n"))
        );
        showErrorDialog(form.getContainer(), message, "Export error report");
      }
    }).start());
  }

  public void updateForms() {
    forms.merge(getFormsFromStorage());
    form.refresh();
  }

  private List<FormStatus> getFormsFromStorage() {
    return FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList());
  }

  public ExportPanelForm getForm() {
    return form;
  }

  private List<String> export() {
    form.disableUI();
    terminationFuture.reset();
    List<String> errors = forms.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> ExportAction.export(formDefinition, forms.getConfiguration(formDefinition.getFormId()), terminationFuture).stream())
        .collect(toList());
    form.enableUI();
    return errors;
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    updateForms();
  }
}
