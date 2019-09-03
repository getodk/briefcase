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

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.load;
import static org.opendatakit.briefcase.operations.export.ExportForms.buildCustomConfPrefix;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer;

public class ExportPanel {
  public static final String TAB_NAME = "Export";

  private final Workspace workspace;
  private final ExportForms forms;
  private final ExportPanelForm form;
  private final BriefcasePreferences appPreferences;
  private final BriefcasePreferences exportPreferences;
  private final BriefcasePreferences pullPanelPrefs;
  private final Analytics analytics;

  ExportPanel(Workspace workspace, ExportForms forms, ExportPanelForm form, BriefcasePreferences appPreferences, BriefcasePreferences exportPreferences, BriefcasePreferences pullPanelPrefs, Analytics analytics) {
    this.workspace = workspace;
    this.forms = forms;
    this.form = form;
    this.appPreferences = appPreferences;
    this.exportPreferences = exportPreferences;
    this.pullPanelPrefs = pullPanelPrefs;
    this.analytics = analytics;
    AnnotationProcessor.process(this);// if not using AOP
    analytics.register(form.getContainer());

    form.onDefaultConfSet(conf -> {
      forms.updateDefaultConfiguration(conf);
      exportPreferences.removeAll(ExportConfiguration.keys());
      exportPreferences.putAll(conf.asMap());
    });

    form.onDefaultConfReset(() -> {
      forms.updateDefaultConfiguration(empty().build());
      exportPreferences.removeAll(ExportConfiguration.keys());
    });

    forms.onSuccessfulExport((FormKey formKey, LocalDateTime exportDateTime) ->
        exportPreferences.put(ExportForms.buildExportDateTimePrefix(formKey.getId()), exportDateTime.format(ISO_DATE_TIME))
    );

    form.onChange(() -> {
      updateCustomConfPreferences();
      updateSelectButtons();
    });

    form.onExport(() -> {
      List<String> errors = getErrors();
      if (errors.isEmpty()) {
        form.setExporting();
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

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    if (!forms.someSelected())
      errors.add("- No forms have been selected. Please, select a form.");

    if (!forms.allSelectedFormsHaveConfiguration())
      errors.add("- Some forms are missing their export directory. Please, ensure that there's a default export directory or that you have set one in all custom configurations.");

    for (FormMetadata formMetadata : forms.getSelectedForms()) {
      ExportConfiguration conf = forms.getConfiguration(formMetadata);

      if (formMetadata.isEncrypted() && !conf.isPemFilePresent())
        errors.add("- The form " + formMetadata.getFormName() + " is encrypted. Please, configure a PEM file.");
    }
    return errors;
  }

  private void updateCustomConfPreferences() {
    // Clean all custom conf keys
    forms.forEach(formMetadata ->
        exportPreferences.removeAll(ExportConfiguration.keys(buildCustomConfPrefix(formMetadata.getKey().getId())))
    );

    // Put custom confs
    forms.getCustomConfigurations().forEach((formKey, configuration) ->
        exportPreferences.putAll(configuration.asMap(buildCustomConfPrefix(formKey.getId())))
    );
  }

  private void updateSelectButtons() {
    if (forms.allSelected()) {
      form.toggleClearAll();
    } else {
      form.toggleSelectAll();
    }
  }

  public static ExportPanel from(Workspace workspace, BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences, BriefcasePreferences pullPrefs, Analytics analytics) {
    ExportConfiguration initialDefaultConf = load(exportPreferences);
    ExportForms forms = ExportForms.load(initialDefaultConf, workspace.formMetadata.fetchAll().collect(toList()), exportPreferences);
    ExportPanelForm form = ExportPanelForm.from(forms, appPreferences, pullPrefs, initialDefaultConf);
    return new ExportPanel(
        workspace,
        forms,
        form,
        appPreferences,
        exportPreferences,
        pullPrefs,
        analytics
    );
  }

  void updateForms() {
    forms.merge(workspace.formMetadata.fetchAll().collect(toList()));
    form.refresh();
  }

  public ExportPanelForm getForm() {
    return form;
  }

  private void export() {
    Stream<Job<?>> allJobs = forms.getSelectedForms().stream().map(formMetadata -> {
      ExportConfiguration configuration = forms.getConfiguration(formMetadata);
      FormDefinition formDef = FormDefinition.from(formMetadata);
      // TODO Abstract away the subtype of RemoteServer. This should say Optional<RemoteServer>
      Optional<AggregateServer> savedPullSource = RemoteServer.readFromPrefs(appPreferences, pullPanelPrefs, formMetadata.getKey());

      Job<Void> pullJob = configuration.resolvePullBefore() && savedPullSource.isPresent()
          ? new PullFromAggregate(workspace, savedPullSource.get(), false, EventBus::publish)
          .pull(
              formMetadata, appPreferences.resolveStartFromLast()
                  ? Optional.of(formMetadata.getCursor())
                  : Optional.empty()
          )
          : Job.noOpSupplier();

      Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(workspace, formMetadata, formDef, configuration, analytics));

      Job<Void> exportGeoJsonJob = configuration.resolveIncludeGeoJsonExport()
          ? Job.run(runnerStatus -> ExportToGeoJson.export(workspace, formMetadata, formDef, configuration))
          : Job.noOp;

      return pullJob
          .thenRun(exportJob)
          .thenRun(exportGeoJsonJob);
    });

    form.cleanAllStatusLines();
    JobsRunner.launchAsync(allJobs).onComplete(form::unsetExporting).waitForCompletion();
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
