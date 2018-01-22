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
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.IFormDefinition;

public class ExportForms {
  private static final String EXPORT_DATE_PREFIX = "export_date_";
  private static final String CUSTOM_CONF_PREFIX = "custom_";
  private final List<FormStatus> forms;
  private final Map<String, ExportConfiguration> configurations;
  private final Map<String, LocalDateTime> lastExportDateTimes;
  private final List<BiConsumer<String, LocalDateTime>> onSuccessfulExportCallbacks = new ArrayList<>();
  private Map<String, FormStatus> formsIndex = new HashMap<>();

  public ExportForms(List<FormStatus> forms, Map<String, ExportConfiguration> configurations, Map<String, LocalDateTime> lastExportDateTimes) {
    this.forms = forms;
    this.configurations = configurations;
    this.lastExportDateTimes = lastExportDateTimes;
    rebuildIndex();
  }

  public static ExportForms empty() {
    return new ExportForms(new ArrayList<>(), new HashMap<>(), new HashMap<>());
  }

  public static ExportForms load(List<FormStatus> forms, BriefcasePreferences preferences) {
    // This should be a simple Map filtering block but we'll have to wait for Vavr.io

    Map<String, ExportConfiguration> configurations = new HashMap<>();
    Map<String, LocalDateTime> lastExportDateTimes = new HashMap<>();
    forms.forEach(form -> {
      String formId = getFormId(form);
      configurations.put(formId, ExportConfiguration.load(preferences, buildCustomConfPrefix(formId)));
      preferences.nullSafeGet(buildExportDateTimePrefix(formId))
          .map(LocalDateTime::parse).ifPresent(dateTime -> lastExportDateTimes.put(formId, dateTime));
    });
    return new ExportForms(
        forms,
        configurations,
        lastExportDateTimes
    );
  }

  private static String getFormId(IFormDefinition formDefinition) {
    return formDefinition.getFormId();
  }

  private static String getFormId(FormStatus form) {
    return getFormId(form.getFormDefinition());
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

  public boolean hasConfiguration(FormStatus form) {
    return configurations.containsKey(getFormId(form)) && configurations.get(getFormId(form)).isValid();
  }

  public Optional<ExportConfiguration> getConfiguration(FormStatus form) {
    return Optional.ofNullable(configurations.get(getFormId(form)));
  }

  public Optional<ExportConfiguration> getConfiguration(IFormDefinition formDefinition) {
    return Optional.ofNullable(configurations.get(getFormId(formDefinition)));
  }

  public void removeConfiguration(FormStatus form) {
    configurations.remove(getFormId(form));
  }

  public void setConfiguration(FormStatus form, ExportConfiguration configuration) {
    configurations.put(getFormId(form), configuration);
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

  public boolean allSelectedFormsHaveConfiguration() {
    return getSelectedForms().stream()
        .map(ExportForms::getFormId)
        .allMatch(formId -> configurations.containsKey(formId) && !configurations.get(formId).isEmpty());
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

  public Map<String, ExportConfiguration> getValidConfigurations() {
    return configurations.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty() && entry.getValue().isValid())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Optional<LocalDateTime> getLastExportDateTime(FormStatus form) {
    return Optional.ofNullable(lastExportDateTimes.get(getFormId(form)));
  }

  public void onSuccessfulExport(BiConsumer<String, LocalDateTime> callback) {
    onSuccessfulExportCallbacks.add(callback);
  }

  private FormStatus getForm(IFormDefinition formDefinition) {
    String formId = getFormId(formDefinition);
    return Optional.ofNullable(formsIndex.get(formId))
        .orElseThrow(() -> new RuntimeException("Form with form ID " + formId + " not found"));
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(ExportForms::getFormId, form -> form));
  }
}
