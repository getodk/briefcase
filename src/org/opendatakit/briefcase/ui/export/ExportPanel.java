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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.ExportForms.buildCustomConfPrefix;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.export.ExportToCsv;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.SavePasswordsConsentGiven;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.transfer.NewTransferAction;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
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
  private final FormCache formCache;

  ExportPanel(ExportForms forms, ExportPanelForm form, BriefcasePreferences appPreferences, BriefcasePreferences preferences, Analytics analytics, FormCache formCache) {
    this.forms = forms;
    this.form = form;
    this.appPreferences = appPreferences;
    this.preferences = preferences;
    this.formCache = formCache;
    AnnotationProcessor.process(this);// if not using AOP
    analytics.register(form.getContainer());

    form.getConfPanel().onChange(() ->
        forms.updateDefaultConfiguration(form.getConfPanel().getConfiguration())
    );

    forms.onSuccessfulExport((String formId, LocalDateTime exportDateTime) ->
        preferences.put(ExportForms.buildExportDateTimePrefix(formId), exportDateTime.format(ISO_DATE_TIME))
    );

    form.onChange(() -> {
      updatePreferences();
      updateExportButton();
      updateSelectButtons();
    });

    form.onExport(() -> {
      form.setExporting();
      List<String> errors = getErrors();
      if (errors.isEmpty()) {
        new Thread(this::export).start();
      } else {
        analytics.event("Export", "Export", "Configuration errors", null);
        showErrorDialog(getForm().getContainer(), errors.stream().collect(joining("\n")), "Export errors");
        form.unsetExporting();
      }
    });

    updateExportButton();
    updateSelectButtons();
  }

  private List<String> getErrors() {
    // Segregating this validation from the export process to move it to ExportConfiguration on the future
    return forms.getSelectedForms().stream().flatMap(formStatus -> {
      ExportConfiguration exportConfiguration = forms.getConfiguration(formStatus.getFormDefinition().getFormId());
      boolean needsPemFile = ((BriefcaseFormDefinition) formStatus.getFormDefinition()).isFileEncryptedForm() || ((BriefcaseFormDefinition) formStatus.getFormDefinition()).isFieldEncryptedForm();

      if (needsPemFile && !exportConfiguration.isPemFilePresent())
        return Stream.of("The form " + formStatus.getFormName() + " is encrypted and you haven't set a PEM file");
      if (needsPemFile)
        return ExportConfiguration.readPemFile(exportConfiguration.getPemFile()
            .orElseThrow(() -> new RuntimeException("PEM file not present"))
        ).getErrors().stream();
      return Stream.empty();
    }).collect(toList());
  }

  private void updatePreferences() {
    // Clean all default conf keys
    preferences.removeAll(ExportConfiguration.keys());

    // Put default conf
    if (form.getConfPanel().isValid())
      preferences.putAll(form.getConfPanel().getConfiguration().asMap());

    // Clean all custom conf keys
    forms.forEach(formId ->
        preferences.removeAll(ExportConfiguration.keys(buildCustomConfPrefix(formId)))
    );

    // Put custom confs
    forms.getCustomConfigurations().forEach((formId, configuration) ->
        preferences.putAll(configuration.asMap(buildCustomConfPrefix(formId)))
    );
  }

  private void updateExportButton() {
    if (forms.someSelected() && (form.getConfPanel().isValid() || forms.allSelectedFormsHaveConfiguration()))
      form.enableExport();
    else
      form.disableExport();
  }

  private void updateSelectButtons() {
    if (forms.allSelected()) {
      form.toggleClearAll();
    } else {
      form.toggleSelectAll();
    }
  }

  public static ExportPanel from(BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences, Analytics analytics, FormCache formCache) {
    ExportConfiguration defaultConfiguration = ExportConfiguration.load(exportPreferences);
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(defaultConfiguration, BriefcasePreferences.getStorePasswordsConsentProperty(), true);
    ExportForms forms = ExportForms.load(defaultConfiguration, toFormStatuses(formCache.getForms()), exportPreferences, appPreferences);
    ExportPanelForm form = ExportPanelForm.from(forms, confPanel);
    return new ExportPanel(
        forms,
        form,
        appPreferences,
        exportPreferences,
        analytics,
        formCache
    );
  }

  public void updateForms() {
    forms.merge(toFormStatuses(formCache.getForms()));
    form.refresh();
  }

  private static List<FormStatus> toFormStatuses(List<BriefcaseFormDefinition> formDefs) {
    return formDefs.stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList());
  }

  public ExportPanelForm getForm() {
    return form;
  }

  private void export() {
    try {
      forms.getSelectedForms()
          .parallelStream()
          .peek(FormStatus::clearStatusHistory)
          .forEach(form -> {
            String formId = form.getFormDefinition().getFormId();
            ExportConfiguration configuration = forms.getConfiguration(formId);
            if (configuration.resolvePullBefore())
              forms.getTransferSettings(formId).ifPresent(sci -> NewTransferAction.transferServerToBriefcase(
                  sci,
                  new TerminationFuture(),
                  Collections.singletonList(form),
                  appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new)
              ));
            BriefcaseFormDefinition formDefinition = (BriefcaseFormDefinition) form.getFormDefinition();
            ExportToCsv.export(FormDefinition.from(formDefinition), configuration, true);
          });
    } catch (Throwable t) {
      log.error("Error while exporting forms", t);
      showErrorDialog(getForm().getContainer(), "Unexpected error. See logs", "Export errors");
    } finally {
      form.unsetExporting();
    }
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
    form.getConfPanel().savePasswordsConsentGiven();
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    forms.flushTransferSettings();
    form.getConfPanel().savePasswordsConsentRevoked();
  }
}
