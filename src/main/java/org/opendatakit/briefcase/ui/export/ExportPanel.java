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

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.load;
import static org.opendatakit.briefcase.export.ExportForms.buildCustomConfPrefix;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportEvent;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.export.ExportToCsv;
import org.opendatakit.briefcase.export.ExportToGeoJson;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;
import org.opendatakit.briefcase.ui.reused.Analytics;

public class ExportPanel {
  public static final String TAB_NAME = "Export";

  private final ExportForms forms;
  private final ExportPanelForm form;
  private final BriefcasePreferences appPreferences;
  private final BriefcasePreferences exportPreferences;
  private final BriefcasePreferences pullPanelPrefs;
  private final Analytics analytics;
  private final Http http;
  private final FormMetadataPort formMetadataPort;

  ExportPanel(ExportForms forms, ExportPanelForm form, BriefcasePreferences appPreferences, BriefcasePreferences exportPreferences, BriefcasePreferences pullPanelPrefs, Analytics analytics, Http http, FormMetadataPort formMetadataPort) {
    this.forms = forms;
    this.form = form;
    this.appPreferences = appPreferences;
    this.exportPreferences = exportPreferences;
    this.pullPanelPrefs = pullPanelPrefs;
    this.analytics = analytics;
    this.http = http;
    this.formMetadataPort = formMetadataPort;
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

    forms.onSuccessfulExport((String formId, LocalDateTime exportDateTime) ->
        exportPreferences.put(ExportForms.buildExportDateTimePrefix(formId), exportDateTime.format(ISO_DATE_TIME))
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

    for (FormStatus formStatus : forms.getSelectedForms()) {
      ExportConfiguration conf = forms.getConfiguration(formStatus.getFormId());

      if (formStatus.isEncrypted() && !conf.isPemFilePresent())
        errors.add("- The form " + formStatus.getFormName() + " is encrypted. Please, configure a PEM file.");
    }
    return errors;
  }

  private void updateCustomConfPreferences() {
    // Clean all custom conf keys
    forms.forEach(formId ->
        exportPreferences.removeAll(ExportConfiguration.keys(buildCustomConfPrefix(formId)))
    );

    // Put custom confs
    forms.getCustomConfigurations().forEach((formId, configuration) ->
        exportPreferences.putAll(configuration.asMap(buildCustomConfPrefix(formId)))
    );
  }

  private void updateSelectButtons() {
    if (forms.allSelected()) {
      form.toggleClearAll();
    } else {
      form.toggleSelectAll();
    }
  }

  public static ExportPanel from(BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences, BriefcasePreferences pullPrefs, Analytics analytics, Http http, FormMetadataPort formMetadataPort) {
    ExportConfiguration initialDefaultConf = load(exportPreferences);
    ExportForms forms = ExportForms.load(initialDefaultConf, formMetadataPort.fetchAll().map(FormStatus::new).collect(toList()), exportPreferences);
    ExportPanelForm form = ExportPanelForm.from(forms, appPreferences, pullPrefs, initialDefaultConf);
    return new ExportPanel(
        forms,
        form,
        appPreferences,
        exportPreferences,
        pullPrefs,
        analytics,
        http,
        formMetadataPort
    );
  }

  void updateForms() {
    forms.merge(formMetadataPort.fetchAll().map(FormStatus::new).collect(toList()));
    form.refresh();
  }

  public ExportPanelForm getForm() {
    return form;
  }

  private void export() {
    Stream<Job<?>> allJobs = forms.getSelectedForms().stream().map(form -> {
      FormKey key = FormKey.from(form);
      FormMetadata formMetadata = formMetadataPort.fetch(key).orElseThrow(BriefcaseException::new);
      form.setStatusString("Starting to export form");
      String formId = form.getFormId();
      ExportConfiguration configuration = forms.getConfiguration(formId);
      FormDefinition formDef = FormDefinition.from(formMetadata);
      // TODO Abstract away the subtype of RemoteServer. This should say Optional<RemoteServer>
      Optional<AggregateServer> savedPullSource = RemoteServer.readFromPrefs(appPreferences, pullPanelPrefs, form);

      Job<Void> pullJob = configuration.resolvePullBefore() && savedPullSource.isPresent()
          ? new PullFromAggregate(http, savedPullSource.get(), false, EventBus::publish, formMetadataPort)
          .pull(
              formMetadata, appPreferences.resolveStartFromLast()
                  ? Optional.of(formMetadata.getCursor())
                  : Optional.empty()
          )
          : Job.noOpSupplier();

      Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(formMetadataPort, formMetadata, formDef, configuration, analytics));

      Job<Void> exportGeoJsonJob = configuration.resolveIncludeGeoJsonExport()
          ? Job.run(runnerStatus -> ExportToGeoJson.export(formMetadata, formDef, configuration, analytics))
          : Job.noOp;

      return Job
          .run(runnerStatus -> form.clearStatusHistory())
          .thenRun(pullJob)
          .thenRun(exportJob)
          .thenRun(exportGeoJsonJob);
    });

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