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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class ExportForms {
  private List<FormMetadata> forms;
  private final Set<FormKey> selectedForms = new HashSet<>();

  public ExportForms(List<FormMetadata> forms) {
    this.forms = forms;
  }

  public void refresh(List<FormMetadata> incomingForms) {
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

}
