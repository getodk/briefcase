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
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class ExportPanel {
  public static final String TAB_NAME = "Export";

  private final TerminationFuture terminationFuture;
  private final ExportForms forms;
  private final ExportPanelView view;

  public ExportPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    forms = new ExportForms();

    view = new ExportPanelView(forms, ExportConfiguration.empty());
    view.onExport(defaultConfiguration -> new Thread(() -> {
      List<String> errors = export(defaultConfiguration);
      if (!errors.isEmpty()) {
        String message = String.format("%s\n\n%s", "We have found some errors while performing the requested export actions:", errors.stream().map(e -> "- " + e).collect(joining("\n")));
        showErrorDialog(view, message, "Export error report");
      }
    }).start());

    updateForms();
  }

  public ExportPanelView getView() {
    return view;
  }

  private List<String> export(ExportConfiguration defaultConfiguration) {
    view.disableUI();
    terminationFuture.reset();
    List<String> errors = forms.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(defaultConfiguration, formDefinition).stream())
        .collect(toList());
    view.enableUI();
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
            conf.mapDateRangeStart((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null),
            conf.mapDateRangeEnd((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null)
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }

  public void updateForms() {
    List<FormStatus> incomingForms = FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList());

    forms.merge(incomingForms);
    view.refresh();
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void onTransferSucceededEvent(TransferSucceededEvent event) {
    updateForms();
  }
}
