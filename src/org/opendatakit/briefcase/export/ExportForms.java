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
package org.opendatakit.briefcase.export;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.IFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class ExportForms {
  private static final String EXPORT_DATE_PREFIX = "export_date_";
  private static final String CUSTOM_CONF_PREFIX = "custom_";
  private final List<FormStatus> forms;
  private ExportConfiguration defaultConfiguration;
  private final Map<String, ExportConfiguration> customConfigurations;
  private final Map<String, LocalDateTime> lastExportDateTimes;
  private final Map<String, ServerConnectionInfo> transferSettings;
  private final List<BiConsumer<String, LocalDateTime>> onSuccessfulExportCallbacks = new ArrayList<>();
  private Map<String, FormStatus> formsIndex = new HashMap<>();

  public ExportForms(List<FormStatus> forms, ExportConfiguration defaultConfiguration, Map<String, ExportConfiguration> configurations, Map<String, LocalDateTime> lastExportDateTimes, Map<String, ServerConnectionInfo> transferSettings) {
    this.forms = forms;
    this.defaultConfiguration = defaultConfiguration;
    this.customConfigurations = configurations;
    this.lastExportDateTimes = lastExportDateTimes;
    this.transferSettings = transferSettings;
    rebuildIndex();
  }

  public static ExportForms load(ExportConfiguration defaultConfiguration, List<FormStatus> forms, BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences) {
    // This should be a simple Map filtering block but we'll have to wait for Vavr.io

    Map<String, ExportConfiguration> configurations = new HashMap<>();
    Map<String, LocalDateTime> lastExportDateTimes = new HashMap<>();
    Map<String, ServerConnectionInfo> transferSettings = new HashMap<>();
    forms.forEach(form -> {
      String formId = getFormId(form);
      ExportConfiguration load = ExportConfiguration.loadFormConfig(exportPreferences, buildCustomConfPrefix(formId));
      if (!load.isEmpty()) {
        configurations.put(formId, load);
      }
      exportPreferences.nullSafeGet(buildExportDateTimePrefix(formId))
          .map(LocalDateTime::parse)
          .ifPresent(dateTime -> lastExportDateTimes.put(formId, dateTime));
      String urlKey = String.format("%s_pull_settings_url", formId);
      String usernameKey = String.format("%s_pull_settings_username", formId);
      String passwordKey = String.format("%s_pull_settings_password", formId);
      if (appPreferences.hasKey(urlKey) && appPreferences.hasKey(usernameKey) && appPreferences.hasKey(passwordKey))
        transferSettings.put(formId, new ServerConnectionInfo(
            appPreferences.nullSafeGet(urlKey)
                .orElseThrow(() -> new RuntimeException("Null value saved for " + urlKey)),
            appPreferences.nullSafeGet(usernameKey)
                .orElseThrow(() -> new RuntimeException("Null value saved for " + usernameKey)),
            appPreferences.nullSafeGet(passwordKey)
                .orElseThrow(() -> new RuntimeException("Null value saved for " + passwordKey)).toCharArray()
        ));

    });
    return new ExportForms(
        forms,
        defaultConfiguration,
        configurations,
        lastExportDateTimes,
        transferSettings
    );
  }

  private static String getFormId(FormStatus form) {
    return form.getFormDefinition().getFormId();
  }

  public static String buildExportDateTimePrefix(String formId) {
    return EXPORT_DATE_PREFIX + formId;
  }

  public static String buildCustomConfPrefix(String formId) {
    return CUSTOM_CONF_PREFIX + formId + "_";
  }

  public void merge(List<FormStatus> forms) {
    this.forms.addAll(forms.stream().filter(form -> !formsIndex.containsKey(getFormId(form))).collect(toList()));
    rebuildIndex();
  }

  public int size() {
    return forms.size();
  }

  public FormStatus get(int rowIndex) {
    return forms.get(rowIndex);
  }

  public void forEach(Consumer<String> callback) {
    forms.stream().map(ExportForms::getFormId).forEach(callback);
  }

  public boolean hasConfiguration(FormStatus form) {
    return customConfigurations.containsKey(getFormId(form));
  }

  public Optional<ExportConfiguration> getCustomConfiguration(FormStatus form) {
    return Optional.ofNullable(customConfigurations.get(getFormId(form)));
  }

  public Map<String, ExportConfiguration> getCustomConfigurations() {
    return customConfigurations;
  }

  public void updateDefaultConfiguration(ExportConfiguration configuration) {
    defaultConfiguration = configuration;
  }

  public ExportConfiguration getConfiguration(FormStatus form) {
    return Optional.ofNullable(customConfigurations.get(form.getFormDefinition().getFormId()))
        .orElse(ExportConfiguration.empty())
        .fallingBackTo(defaultConfiguration, form);
  }

  public void removeConfiguration(FormStatus form) {
    customConfigurations.remove(getFormId(form));
  }

  public void putConfiguration(FormStatus form, ExportConfiguration configuration) {
    customConfigurations.put(getFormId(form), configuration);
  }

  public boolean hasTransferSettings(FormStatus form) {
    return transferSettings.containsKey(form.getFormDefinition().getFormId());
  }

  public Optional<ServerConnectionInfo> getTransferSettings(String formId) {
    return Optional.ofNullable(transferSettings.get(formId));
  }

  public void putTransferSettings(FormStatus form, ServerConnectionInfo transferSettings) {
    this.transferSettings.put(getFormId(form), transferSettings);
  }

  public boolean allSelectedFormsHaveConfiguration() {
    return getSelectedForms().stream()
        .map(this::getConfiguration)
        .allMatch(ExportConfiguration::isValid);
  }

  public void selectAll() {
    forms.forEach(form -> form.setSelected(true));
  }

  public void clearAll() {
    forms.forEach(form -> form.setSelected(false));
  }

  public List<FormStatus> getSelectedForms() {
    return forms.stream().filter(FormStatus::isSelected).collect(toList());
  }

  public boolean someSelected() {
    return !getSelectedForms().isEmpty();
  }

  public boolean allSelected() {
    return forms.stream().allMatch(FormStatus::isSelected);
  }

  public boolean noneSelected() {
    return forms.stream().noneMatch(FormStatus::isSelected);
  }

  public void appendStatus(IFormDefinition formDefinition, String statusUpdate, boolean successful) {
    FormStatus form = getForm(formDefinition);
    form.setStatusString(statusUpdate, successful);
    if (successful) {
      LocalDateTime exportDate = LocalDateTime.now();
      String formId = getFormId(form);
      lastExportDateTimes.put(formId, exportDate);
      onSuccessfulExportCallbacks.forEach(callback -> callback.accept(formId, exportDate));
    }
  }

  public Optional<LocalDateTime> getLastExportDateTime(FormStatus form) {
    return Optional.ofNullable(lastExportDateTimes.get(getFormId(form)));
  }

  public void onSuccessfulExport(BiConsumer<String, LocalDateTime> callback) {
    onSuccessfulExportCallbacks.add(callback);
  }

  private FormStatus getForm(IFormDefinition formDefinition) {
    return getForm(formDefinition.getFormId());
  }

  private FormStatus getForm(String formId) {
    return Optional.ofNullable(formsIndex.get(formId))
        .orElseThrow(() -> new RuntimeException("Form with form ID " + formId + " not found"));
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(ExportForms::getFormId, form -> form));
  }

  public void flushTransferSettings() {
    transferSettings.clear();
  }
}
