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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportType;
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

  public ExportPanel(TerminationFuture terminationFuture) {
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    forms = new ExportForms();

    ConfigurationPanel confPanel = ConfigurationPanel.from(ExportConfiguration.empty());

    form = ExportPanelForm.from(forms, confPanel);

    form.onChange(() -> {
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
      List<String> errors = export(confPanel.getConfiguration());
      if (!errors.isEmpty()) {
        String message = String.format("%s\n\n%s", "We have found some errors while performing the requested export actions:", errors.stream().map(e -> "- " + e).collect(joining("\n")));
        showErrorDialog(form, message, "Export error report");
      }
    }).start());

    updateForms();
  }

  public void updateForms() {
    forms.merge(FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList()));
    form.refresh();
  }

  public JPanel getForm() {
    return form.container;
  }

  private List<String> export(ExportConfiguration defaultConfiguration) {
    form.disableUI();
    terminationFuture.reset();
    List<String> errors = forms.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(defaultConfiguration, formDefinition).stream())
        .collect(toList());
    form.enableUI();
    return errors;
  }

  private List<String> export(ExportConfiguration defaultConfiguration, BriefcaseFormDefinition formDefinition) {
    List<String> errors = new ArrayList<>();
    ExportConfiguration conf = forms.getConfiguration(formDefinition).orElse(defaultConfiguration);
    Optional<File> pemFile = conf.mapPemFile(Path::toFile).filter(File::exists);
    if ((formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) && !pemFile.isPresent())
      errors.add(formDefinition.getFormName() + " form requires is encrypted and you haven't defined a valid private key file location");
    else
      try {
        ExportAction.export(
            conf.mapExportDir(Path::toFile).orElseThrow(() -> new RuntimeException("Wrong export configuration")),
            ExportType.CSV,
            formDefinition,
            pemFile.orElse(null),
            terminationFuture,
            conf.mapStartDate((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null),
            conf.mapEndDate((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null)
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }



  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    updateForms();
  }
}
