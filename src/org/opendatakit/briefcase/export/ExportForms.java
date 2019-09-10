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
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.opendatakit.briefcase.export.ExportConfiguration.Builder;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;

public class ExportForms {
  private static final String EXPORT_DATE_PREFIX = "export_date_";
  private static final String CUSTOM_CONF_PREFIX = "custom_";
  private final Map<String, ExportConfiguration> customConfigurations;
  private final Map<String, LocalDateTime> lastExportDateTimes;
  private final List<BiConsumer<String, LocalDateTime>> onSuccessfulExportCallbacks = new ArrayList<>();
  private ExportConfiguration defaultConfiguration;
  private List<FormStatus> forms;
  private Map<String, FormStatus> formsIndex = new HashMap<>();

  public ExportForms(List<FormStatus> forms, ExportConfiguration defaultConfiguration, Map<String, ExportConfiguration> configurations, Map<String, LocalDateTime> lastExportDateTimes) {
    this.forms = forms;
    this.defaultConfiguration = defaultConfiguration;
    this.customConfigurations = configurations;
    this.lastExportDateTimes = lastExportDateTimes;
    rebuildIndex();
  }

  public static ExportForms load(ExportConfiguration defaultConfiguration, List<FormStatus> forms, BriefcasePreferences exportPreferences) {
    // This should be a simple Map filtering block but we'll have to wait for Vavr.io

    Map<String, ExportConfiguration> configurations = new HashMap<>();
    Map<String, LocalDateTime> lastExportDateTimes = new HashMap<>();
    forms.forEach(form -> {
      String formId = getFormId(form);
      ExportConfiguration load = Builder.load(exportPreferences, buildCustomConfPrefix(formId));
      if (!load.isEmpty())
        configurations.put(formId, load);
      exportPreferences.nullSafeGet(buildExportDateTimePrefix(formId))
          .map(LocalDateTime::parse)
          .ifPresent(dateTime -> lastExportDateTimes.put(formId, dateTime));
    });
    return new ExportForms(
        forms,
        defaultConfiguration,
        configurations,
        lastExportDateTimes
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

  public void merge(List<FormStatus> incomingForms) {
    forms = new ArrayList<>(incomingForms);
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

  public ExportConfiguration getConfiguration(String formId) {
    return Optional.ofNullable(customConfigurations.get(formId))
        .orElse(empty().build())
        .fallingBackTo(defaultConfiguration);
  }

  public void removeConfiguration(FormStatus form) {
    customConfigurations.remove(getFormId(form));
  }

  public void putConfiguration(FormStatus form, ExportConfiguration configuration) {
    customConfigurations.put(getFormId(form), configuration);
  }

  public boolean allSelectedFormsHaveConfiguration() {
    return getSelectedForms().stream()
        .map(ExportForms::getFormId)
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
    return !forms.isEmpty() && forms.stream().allMatch(FormStatus::isSelected);
  }

  public boolean isEmpty() {
    return forms.isEmpty();
  }

  public boolean noneSelected() {
    return forms.stream().noneMatch(FormStatus::isSelected);
  }

  public void appendStatus(ExportEvent event) {
    getForm(event.getFormId()).setStatusString(event.getStatusLine());
    if (event.isSuccess()) {
      LocalDateTime exportDate = LocalDateTime.now();
      lastExportDateTimes.put(event.getFormId(), exportDate);
      onSuccessfulExportCallbacks.forEach(callback -> callback.accept(event.getFormId(), exportDate));
    }
  }

  public Optional<LocalDateTime> getLastExportDateTime(FormStatus form) {
    return Optional.ofNullable(lastExportDateTimes.get(getFormId(form)));
  }

  public void onSuccessfulExport(BiConsumer<String, LocalDateTime> callback) {
    onSuccessfulExportCallbacks.add(callback);
  }

  private FormStatus getForm(String formId) {
    return Optional.ofNullable(formsIndex.get(formId))
        .orElseThrow(() -> new RuntimeException("Form with form ID " + formId + " not found"));
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(ExportForms::getFormId, form -> form));
  }
}
