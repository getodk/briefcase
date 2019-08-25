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
package org.opendatakit.briefcase.operations.export;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.opendatakit.briefcase.operations.export.ExportConfiguration.Builder;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;

public class ExportForms {
  private static final String EXPORT_DATE_PREFIX = "export_date_";
  private static final String CUSTOM_CONF_PREFIX = "custom_";
  private List<FormMetadata> forms;
  private ExportConfiguration defaultConfiguration;
  private final Map<FormKey, ExportConfiguration> customConfigurations;
  private final Map<FormKey, LocalDateTime> lastExportDateTimes;
  private final List<BiConsumer<FormKey, LocalDateTime>> onSuccessfulExportCallbacks = new ArrayList<>();
  private final Set<FormKey> selectedForms = new HashSet<>();

  public ExportForms(List<FormMetadata> forms, ExportConfiguration defaultConfiguration, Map<FormKey, ExportConfiguration> configurations, Map<FormKey, LocalDateTime> lastExportDateTimes) {
    this.forms = forms;
    this.defaultConfiguration = defaultConfiguration;
    this.customConfigurations = configurations;
    this.lastExportDateTimes = lastExportDateTimes;
  }

  public static ExportForms load(ExportConfiguration defaultConfiguration, List<FormMetadata> formMetadataList, BriefcasePreferences exportPreferences) {
    // This should be a simple Map filtering block but we'll have to wait for Vavr.io

    Map<FormKey, ExportConfiguration> configurations = new HashMap<>();
    Map<FormKey, LocalDateTime> lastExportDateTimes = new HashMap<>();
    formMetadataList.forEach(formMetadata -> {
      ExportConfiguration load = Builder.load(exportPreferences, buildCustomConfPrefix(formMetadata.getKey().getId()));
      if (!load.isEmpty())
        configurations.put(formMetadata.getKey(), load);
      exportPreferences.nullSafeGet(buildExportDateTimePrefix(formMetadata.getKey().getId()))
          .map(LocalDateTime::parse)
          .ifPresent(dateTime -> lastExportDateTimes.put(formMetadata.getKey(), dateTime));
    });
    return new ExportForms(
        formMetadataList,
        defaultConfiguration,
        configurations,
        lastExportDateTimes
    );
  }

  public static String buildExportDateTimePrefix(String formId) {
    return EXPORT_DATE_PREFIX + formId;
  }

  public static String buildCustomConfPrefix(String formId) {
    return CUSTOM_CONF_PREFIX + formId + "_";
  }

  public void merge(List<FormMetadata> incomingForms) {
    forms = new ArrayList<>(incomingForms);
  }

  public int size() {
    return forms.size();
  }

  public FormMetadata get(int rowIndex) {
    return forms.get(rowIndex);
  }

  public void forEach(Consumer<FormMetadata> callback) {
    forms.forEach(callback);
  }

  public boolean hasConfiguration(FormKey formKey) {
    return customConfigurations.containsKey(formKey);
  }

  public Optional<ExportConfiguration> getCustomConfiguration(FormKey formKey) {
    return Optional.ofNullable(customConfigurations.get(formKey));
  }

  public Map<FormKey, ExportConfiguration> getCustomConfigurations() {
    return customConfigurations;
  }

  public void updateDefaultConfiguration(ExportConfiguration configuration) {
    defaultConfiguration = configuration;
  }

  public ExportConfiguration getConfiguration(FormMetadata formMetadata) {
    return Optional.ofNullable(customConfigurations.get(formMetadata.getKey()))
        .orElse(empty().build())
        .fallingBackTo(defaultConfiguration);
  }

  public void removeConfiguration(FormKey formKey) {
    customConfigurations.remove(formKey);
  }

  public void putConfiguration(FormKey formKey, ExportConfiguration configuration) {
    customConfigurations.put(formKey, configuration);
  }

  public boolean allSelectedFormsHaveConfiguration() {
    return getSelectedForms().stream()
        .map(this::getConfiguration)
        .allMatch(ExportConfiguration::isValid);
  }

  public void setSelected(FormKey formKey, boolean selected) {
    if (selected)
      selectedForms.add(formKey);
    else
      selectedForms.remove(formKey);
  }

  public boolean isSelected(FormKey formKey) {
    return selectedForms.contains(formKey);
  }

  public void selectAll() {
    selectedForms.clear();
    selectedForms.addAll(forms.stream().map(FormMetadata::getKey).collect(toList()));
  }

  public void clearAll() {
    selectedForms.clear();
  }

  public List<FormMetadata> getSelectedForms() {
    return forms.stream().filter(formStatus -> selectedForms.contains(formStatus.getKey())).collect(toList());
  }

  public boolean someSelected() {
    return !getSelectedForms().isEmpty();
  }

  public boolean allSelected() {
    return !forms.isEmpty() && forms.size() == selectedForms.size();
  }

  public boolean noneSelected() {
    return !forms.isEmpty() && selectedForms.isEmpty();
  }

  public void appendStatus(ExportEvent event) {
    if (event.isSuccess()) {
      LocalDateTime exportDate = LocalDateTime.now();
      lastExportDateTimes.put(event.getFormKey(), exportDate);
      onSuccessfulExportCallbacks.forEach(callback -> callback.accept(event.getFormKey(), exportDate));
    }
  }

  public Optional<LocalDateTime> getLastExportDateTime(FormKey formKey) {
    return Optional.ofNullable(lastExportDateTimes.get(formKey));
  }

  public void onSuccessfulExport(BiConsumer<FormKey, LocalDateTime> callback) {
    onSuccessfulExportCallbacks.add(callback);
  }
}
