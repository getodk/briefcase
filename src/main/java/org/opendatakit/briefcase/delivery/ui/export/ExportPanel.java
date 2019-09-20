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
package org.opendatakit.briefcase.delivery.ui.export;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.operations.transfer.pull.Pull.buildPullJob;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.removeDefaultExportConfiguration;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setDefaultExportConfiguration;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.GET_DEFAULT_EXPORT_CONFIGURATION;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportEvent;
import org.opendatakit.briefcase.operations.export.ExportForms;
import org.opendatakit.briefcase.operations.export.ExportToCsv;
import org.opendatakit.briefcase.operations.export.ExportToGeoJson;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class ExportPanel {
  public static final String TAB_NAME = "Export";

  private final Container container;
  private final ExportForms forms;
  private final ExportPanelForm panel;
  private final Analytics analytics;

  private ExportPanel(Container container, ExportForms forms, ExportPanelForm panel, Analytics analytics) {
    this.container = container;
    this.forms = forms;
    this.panel = panel;
    this.analytics = analytics;
    AnnotationProcessor.process(this);// if not using AOP
    analytics.register(panel.getContainer());

    panel.onDefaultConfSet(conf -> container.preferences.execute(setDefaultExportConfiguration(conf)));

    panel.onDefaultConfReset(() -> container.preferences.execute(removeDefaultExportConfiguration()));

    panel.onConfigurationSet(((formMetadata, conf) ->
        container.formMetadata.execute(upsert(formMetadata.withExportConfiguration(conf)))
    ));

    panel.onConfigurationReset((formMetadata ->
        container.formMetadata.execute(upsert(formMetadata.withoutExportConfiguration()))
    ));

    panel.onChange(this::updateSelectButtons);

    panel.onExport(() -> {
      List<String> errors = getErrors();
      if (errors.isEmpty()) {
        panel.setExporting();
        new Thread(this::export).start();
      } else
        errorMessage(
            "You can't export yet",
            "" +
                "You can't start an export process until you solve these issues:\n" +
                String.join("\n", errors)
        );
    });

    updateSelectButtons();
  }

  private ExportConfiguration resolveConfiguration(FormKey formKey) {
    ExportConfiguration overrideConfiguration = container.formMetadata.getExportConfiguration(formKey);
    ExportConfiguration defaultConfiguration = container.preferences.query(GET_DEFAULT_EXPORT_CONFIGURATION);
    return overrideConfiguration.fallingBackTo(defaultConfiguration);
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (!forms.someSelected())
      errors.add("- No forms have been selected. Please, select a form.");

    for (FormMetadata formMetadata : forms.getSelectedForms()) {
      ExportConfiguration conf = resolveConfiguration(formMetadata.getKey());

      if (!conf.isValid())
        errors.add("- The form " + formMetadata.getFormName() + " has no valid configuration. Please, set a default configuration or review its custom configuration");

      if (formMetadata.isEncrypted() && !conf.isPemFilePresent())
        errors.add("- The form " + formMetadata.getFormName() + " is encrypted. Please, configure a PEM file.");
    }
    return errors;
  }

  private void updateSelectButtons() {
    if (forms.allSelected()) {
      panel.toggleClearAll();
    } else {
      panel.toggleSelectAll();
    }
  }

  public static ExportPanel from(Container container, Analytics analytics) {
    ExportForms forms = new ExportForms(container.formMetadata.fetchAll().collect(toList()));
    ExportPanelForm panel = ExportPanelForm.from(container, forms, container.preferences.query(GET_DEFAULT_EXPORT_CONFIGURATION));
    return new ExportPanel(container, forms, panel, analytics);
  }

  void updateForms() {
    forms.refresh(container.formMetadata.fetchAll().collect(toList()));
    panel.refresh();
  }

  public ExportPanelForm getPanel() {
    return panel;
  }

  private void export() {
    Stream<Job<?>> allJobs = forms.getSelectedForms().stream().map(formMetadata -> {
      ExportConfiguration conf = resolveConfiguration(formMetadata.getKey());

      FormDefinition formDef = FormDefinition.from(formMetadata);
      // TODO Abstract away the subtype of RemoteServer. This should say Optional<RemoteServer>

      Job<Void> pullJob = conf.resolvePullBefore() && formMetadata.getPullSource().isPresent()
          ? buildPullJob(container, formMetadata, EventBus::publish)
          : Job.noOpSupplier();

      Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(container, formMetadata, formDef, conf));

      Job<Void> exportGeoJsonJob = conf.resolveIncludeGeoJsonExport()
          ? Job.run(runnerStatus -> ExportToGeoJson.export(container, formMetadata, formDef, conf))
          : Job.noOp;

      return pullJob
          .thenRun(exportJob)
          .thenRun(exportGeoJsonJob);
    });

    panel.cleanAllStatusLines();
    JobsRunner.launchAsync(allJobs).onComplete(panel::unsetExporting).waitForCompletion();
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onFormPulledSuccessfully(PullEvent.Success event) {
    updateForms();
  }

  @EventSubscriber(eventClass = ExportEvent.Success.class)
  public void onExportSuccess(ExportEvent.Success event) {
    analytics.event("Export", "Export", "Success", null);
  }

  @EventSubscriber(eventClass = ExportEvent.Failure.class)
  public void onExportFailure(ExportEvent.Failure event) {
    analytics.event("Export", "Export", "Failure", null);
  }
}
