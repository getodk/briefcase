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
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class ExportForms {
  private static final String EXPORT_DATE_PREFIX = "export_date_";
  private static final String CUSTOM_CONF_PREFIX = "custom_";
  private final List<FormStatus> forms;
  private ExportConfiguration defaultConfiguration;
  private final Map<String, ExportConfiguration> customConfigurationsByFormName;
  private final Map<String, LocalDateTime> lastExportDateTimes;
  private final Map<String, ServerConnectionInfo> transferSettings;
  private final List<BiConsumer<String, LocalDateTime>> onSuccessfulExportCallbacks = new ArrayList<>();
  private Map<String, FormStatus> formsIndex = new HashMap<>();

  public ExportForms(List<FormStatus> forms, ExportConfiguration defaultConfiguration, Map<String, ExportConfiguration> customConfigurationsByFormName, Map<String, LocalDateTime> lastExportDateTimes, Map<String, ServerConnectionInfo> transferSettings) {
    this.forms = forms;
    this.defaultConfiguration = defaultConfiguration;
    this.customConfigurationsByFormName = customConfigurationsByFormName;
    this.lastExportDateTimes = lastExportDateTimes;
    this.transferSettings = transferSettings;
    rebuildIndex();
  }

  public static ExportForms load(ExportConfiguration defaultConfiguration, List<FormStatus> forms, BriefcasePreferences exportPreferences, BriefcasePreferences appPreferences) {
    // This should be a simple Map filtering block but we'll have to wait for Vavr.io

    Map<String, ExportConfiguration> configurationsByFormName = new HashMap<>();
    Map<String, LocalDateTime> lastExportDateTimes = new HashMap<>();
    Map<String, ServerConnectionInfo> transferSettings = new HashMap<>();
    forms.forEach(form -> {
      ExportConfiguration load = ExportConfiguration.load(exportPreferences, buildCustomConfPrefix(form.getFormName()));
      if (!load.isEmpty())
        configurationsByFormName.put(form.getFormName(), load);
      exportPreferences.nullSafeGet(buildExportDateTimePrefix(form.getFormName()))
          .map(LocalDateTime::parse)
          .ifPresent(dateTime -> lastExportDateTimes.put(form.getFormName(), dateTime));
      String urlKey = String.format("%s_pull_settings_url", form.getFormName());
      String usernameKey = String.format("%s_pull_settings_username", form.getFormName());
      String passwordKey = String.format("%s_pull_settings_password", form.getFormName());
      if (appPreferences.hasKey(urlKey) && appPreferences.hasKey(usernameKey) && appPreferences.hasKey(passwordKey))
        transferSettings.put(form.getFormName(), new ServerConnectionInfo(
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
        configurationsByFormName,
        lastExportDateTimes,
        transferSettings
    );
  }

  public static String buildExportDateTimePrefix(String formName) {
    return EXPORT_DATE_PREFIX + formName;
  }

  public static String buildCustomConfPrefix(String formName) {
    return CUSTOM_CONF_PREFIX + formName + "_";
  }

  public void merge(List<FormStatus> incomingForms) {
    List<String> incomingFormNames = incomingForms.stream().map(FormStatus::getFormName).collect(toList());
    List<FormStatus> formsToAdd = incomingForms.stream().filter(form -> !formsIndex.containsKey(form.getFormName())).collect(toList());
    List<FormStatus> formsToRemove = formsIndex.values().stream().filter(form -> !incomingFormNames.contains(form.getFormName())).collect(toList());
    forms.addAll(formsToAdd);
    forms.removeAll(formsToRemove);
    rebuildIndex();
  }

  public int size() {
    return forms.size();
  }

  public FormStatus get(int rowIndex) {
    return forms.get(rowIndex);
  }

  public void forEach(Consumer<FormStatus> callback) {
    forms.forEach(callback);
  }

  public boolean hasConfiguration(FormStatus form) {
    return customConfigurationsByFormName.containsKey(form.getFormName());
  }

  public Optional<ExportConfiguration> getCustomConfiguration(FormStatus form) {
    return Optional.ofNullable(customConfigurationsByFormName.get(form.getFormName()));
  }

  public Map<String, ExportConfiguration> getCustomConfigurationsByFormName() {
    return customConfigurationsByFormName;
  }

  public void updateDefaultConfiguration(ExportConfiguration configuration) {
    defaultConfiguration = configuration;
  }

  public ExportConfiguration getConfiguration(String formName) {
    return Optional.ofNullable(customConfigurationsByFormName.get(formName))
        .orElse(ExportConfiguration.empty())
        .fallingBackTo(defaultConfiguration);
  }

  public void removeConfiguration(FormStatus form) {
    customConfigurationsByFormName.remove(form.getFormName());
  }

  public void putConfiguration(FormStatus form, ExportConfiguration configuration) {
    customConfigurationsByFormName.put(form.getFormName(), configuration);
  }

  public boolean hasTransferSettings(FormStatus form) {
    return transferSettings.containsKey(form.getFormName());
  }

  public Optional<ServerConnectionInfo> getTransferSettings(String formName) {
    return Optional.ofNullable(transferSettings.get(formName));
  }

  public void putTransferSettings(FormStatus form, ServerConnectionInfo transferSettings) {
    this.transferSettings.put(form.getFormName(), transferSettings);
  }

  public void removeTransferSettings(FormStatus form) {
    transferSettings.remove(form.getFormName());
  }

  public boolean allSelectedFormsHaveConfiguration() {
    return getSelectedForms().stream()
        .map(FormStatus::getFormName)
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

  public void appendStatus(ExportEvent event) {
    getForm(event.getFormName()).setStatusString(event.getStatusLine(), false);
    if (event.isSuccess()) {
      LocalDateTime exportDate = LocalDateTime.now();
      lastExportDateTimes.put(event.getFormName(), exportDate);
      onSuccessfulExportCallbacks.forEach(callback -> callback.accept(event.getFormName(), exportDate));
    }
  }

  public Optional<LocalDateTime> getLastExportDateTime(FormStatus form) {
    return Optional.ofNullable(lastExportDateTimes.get(form.getFormName()));
  }

  public void onSuccessfulExport(BiConsumer<String, LocalDateTime> callback) {
    onSuccessfulExportCallbacks.add(callback);
  }

  private FormStatus getForm(String formName) {
    return Optional.ofNullable(formsIndex.get(formName))
        .orElseThrow(() -> new RuntimeException("Form " + formName + " not found"));
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(FormStatus::getFormName, form -> form));
  }

  public void flushTransferSettings() {
    transferSettings.clear();
  }
}
