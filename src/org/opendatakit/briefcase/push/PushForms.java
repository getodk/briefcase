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
package org.opendatakit.briefcase.push;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.FormStatus;

public class PushForms {
  private List<FormStatus> forms;
  private Map<String, FormStatus> formsIndex = new HashMap<>();
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();

  public PushForms(List<FormStatus> forms) {
    this.forms = forms;
    rebuildIndex();
  }

  public static PushForms empty() {
    return new PushForms(Collections.emptyList());
  }

  public static PushForms from(List<FormStatus> forms) {
    return new PushForms(forms);
  }

  private static String getFormId(FormStatus form) {
    return form.getFormDefinition().getFormId();
  }

  public void load(List<FormStatus> forms) {
    this.forms = forms;
    triggerOnChange();
  }

  public void merge(List<FormStatus> incomingForms) {
    List<String> incomingFormIds = incomingForms.stream().map(PushForms::getFormId).collect(toList());
    List<FormStatus> formsToAdd = incomingForms.stream().filter(form -> !formsIndex.containsKey(getFormId(form))).collect(toList());
    List<FormStatus> formsToRemove = formsIndex.values().stream().filter(form -> !incomingFormIds.contains(getFormId(form))).collect(toList());
    forms.addAll(formsToAdd);
    forms.removeAll(formsToRemove);
    rebuildIndex();
    triggerOnChange();
  }

  public int size() {
    return forms.size();
  }

  public FormStatus get(int rowIndex) {
    return forms.get(rowIndex);
  }

  public void forEach(Consumer<FormStatus> consumer) {
    forms.forEach(consumer);
  }

  public void selectAll() {
    forms.forEach(form -> form.setSelected(true));
    triggerOnChange();
  }

  public void clearAll() {
    forms.forEach(form -> form.setSelected(false));
    triggerOnChange();
  }

  public List<FormStatus> getSelectedForms() {
    return forms.stream().filter(FormStatus::isSelected).collect(toList());
  }

  public boolean someSelected() {
    return !forms.isEmpty() && !getSelectedForms().isEmpty();
  }

  public boolean allSelected() {
    return !forms.isEmpty() && forms.stream().allMatch(FormStatus::isSelected);
  }

  public void clear() {
    forms = Collections.emptyList();
    triggerOnChange();
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(PushForms::getFormId, form -> form));
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  private void triggerOnChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  public boolean isEmpty() {
    return forms.isEmpty();
  }
}
