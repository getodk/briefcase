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

import java.nio.file.Path;
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
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.SavePasswordsConsentGiven;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.pull.PullForm;
import org.opendatakit.briefcase.pull.PullResult;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.util.FormCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportPanel {
  private static final Logger log = LoggerFactory.getLogger(ExportPanel.class);
  public static final String TAB_NAME = "Export";

  private final ExportForms forms;
  private final ExportPanelForm form;
  private final BriefcasePreferences appPreferences;
  private final BriefcasePreferences preferences;
  private final Analytics analytics;
  private final FormCache formCache;
  private final Http http;

  ExportPanel(ExportForms forms, ExportPanelForm form, BriefcasePreferences appPreferences, BriefcasePreferences preferences, Analytics analytics, FormCache formCache, Http http) {
    this.forms = forms;
    this.form = form;
    this.appPreferences = appPreferences;
    this.preferences = preferences;
    this.analytics = analytics;
    this.formCache = formCache;
    this.http = http;
    AnnotationProcessor.process(this);// if not using AOP
    analytics.register(form.getContainer());

    form.onDefaultConfSet(conf -> {
      forms.updateDefaultConfiguration(conf);
      preferences.removeAll(ExportConfiguration.keys());
      preferences.putAll(conf.asMap());
    });

    form.onDefaultConfReset(() -> {
      forms.updateDefaultConfiguration(empty().build());
      preferences.removeAll(ExportConfiguration.keys());
    });

    forms.onSuccessfulExport((String formId, LocalDateTime exportDateTime) ->
        preferences.put(ExportForms.buildExportDateTimePrefix(formId), exportDateTime.format(ISO_DATE_TIME))
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
      ExportConfiguration conf = forms.getConfiguration(formStatus.getFormDefinition().getFormId());
      boolean needsPemFile = ((BriefcaseFormDefinition) formStatus.getFormDefinition()).isFileEncryptedForm() || ((BriefcaseFormDefinition) formStatus.getFormDefinition()).isFieldEncryptedForm();

      if (needsPemFile && !conf.isPemFilePresent())
        errors.add("- The form " + formStatus.getFormName() + " is encrypted. Please, configure a PEM file.");
    }
    return errors;
  }

  private void updateCustomConfPreferences() {
    // Clean all custom conf keys
    forms.forEach(formId ->
        preferences.removeAll(ExportConfiguration.keys(buildCustomConfPrefix(formId)))
    );

    // Put custom confs
    forms.getCustomConfigurations().forEach((formId, configuration) ->
        preferences.putAll(configuration.asMap(buildCustomConfPrefix(formId)))
    );
  }

  private void updateSelectButtons() {
    if (forms.allSelected()) {
      form.toggleClearAll();
    } else {
      form.toggleSelectAll();
    }
  }

  public static ExportPanel from(BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences, Analytics analytics, FormCache formCache, Http http) {
    ExportConfiguration initialDefaultConf = load(exportPreferences);
    ExportForms forms = ExportForms.load(initialDefaultConf, toFormStatuses(formCache.getForms()), exportPreferences, appPreferences);
    ExportPanelForm form = ExportPanelForm.from(forms, appPreferences, initialDefaultConf);
    return new ExportPanel(
        forms,
        form,
        appPreferences,
        exportPreferences,
        analytics,
        formCache,
        http
    );
  }

  void updateForms() {
    forms.merge(toFormStatuses(formCache.getForms()));
    form.refresh();
  }

  private static List<FormStatus> toFormStatuses(List<BriefcaseFormDefinition> formDefs) {
    return formDefs.stream()
        .map(FormStatus::new)
        .collect(toList());
  }

  public ExportPanelForm getForm() {
    return form;
  }

  private void export() {
    Stream<Job<Void>> allJobs = forms.getSelectedForms().stream().map(form -> {
      form.setStatusString("Starting to export form");
      String formId = form.getFormDefinition().getFormId();
      ExportConfiguration configuration = forms.getConfiguration(formId);
      Path briefcaseDir = appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new);
      FormDefinition formDef = FormDefinition.from((BriefcaseFormDefinition) form.getFormDefinition());
      Optional<RemoteServer> server = forms.getTransferSettings(formId).map(RemoteServer::from);

      Job<PullResult> pullJob = configuration.resolvePullBefore() && server.isPresent()
          ? PullForm.pull(http, server.get(), briefcaseDir, false, EventBus::publish, form)
          : Job.noOpSupplier();

      Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(formDef, configuration, analytics));

      Job<Void> exportGeoJsonJob = configuration.resolveIncludeGeoJsonExport()
          ? Job.run(runnerStatus -> ExportToGeoJson.export(formDef, configuration, analytics))
          : Job.noOp;

      return Job
          .run(runnerStatus -> form.clearStatusHistory())
          .thenRun(pullJob)
          .thenRun(exportJob)
          .thenRun(exportGeoJsonJob);
    });
    new JobsRunner<Void>()
        .onError(e -> log.error("Error exporting form", e))
        .onSuccess(__ -> form.unsetExporting())
        .launchAsync(allJobs);
  }

  @EventSubscriber(eventClass = CacheUpdateEvent.class)
  public void onCacheUpdateEvent(CacheUpdateEvent event) {
    updateForms();
  }

  @EventSubscriber(eventClass = PullEvent.NewForm.class)
  public void onNewFormPulledEvent(PullEvent.NewForm event) {
    if (BriefcasePreferences.getStorePasswordsConsentProperty())
      if (event.transferSettings.isPresent())
        forms.putTransferSettings(event.form, event.transferSettings.get());
      else
        forms.removeTransferSettings(event.form);
  }

  @EventSubscriber(eventClass = SavePasswordsConsentGiven.class)
  public void onSavePasswordsConsentGiven(SavePasswordsConsentGiven event) {
    forms.flushTransferSettings();
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    forms.flushTransferSettings();
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
