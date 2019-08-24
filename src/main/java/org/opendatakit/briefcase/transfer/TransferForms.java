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
package org.opendatakit.briefcase.transfer;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;

public class TransferForms implements Iterable<FormMetadata> {
  private List<FormMetadata> formMetadata;
  private Set<FormKey> selectedForms = new HashSet<>();
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();

  private TransferForms(List<FormMetadata> formMetadata) {
    this.formMetadata = formMetadata;
  }

  public static TransferForms empty() {
    return new TransferForms(new ArrayList<>());
  }

  public static TransferForms from(List<FormMetadata> forms) {
    return new TransferForms(forms);
  }

  public static TransferForms of(List<FormMetadata> forms) {
    return new TransferForms(new ArrayList<>(forms));
  }

  public static TransferForms of(FormMetadata... forms) {
    return of(Arrays.asList(forms));
  }

  public void load(List<FormMetadata> forms) {
    this.formMetadata = new ArrayList<>(forms);
    triggerOnChange();
  }

  public void merge(List<FormMetadata> incomingForms) {
    formMetadata = new ArrayList<>(incomingForms);
    triggerOnChange();
  }

  public int size() {
    return formMetadata.size();
  }

  public FormMetadata get(int rowIndex) {
    return formMetadata.get(rowIndex);
  }

  public void selectAll() {
    selectedForms.clear();
    selectedForms.addAll(formMetadata.stream().map(FormMetadata::getKey).collect(toList()));
    triggerOnChange();
  }

  public void clearAll() {
    selectedForms.clear();
    triggerOnChange();
  }

  public boolean isSelected(FormKey formKey) {
    return selectedForms.contains(formKey);
  }

  public void setSelected(FormKey formKey, boolean selected) {
    if (selected)
      selectedForms.add(formKey);
    else
      selectedForms.remove(formKey);
  }

  public TransferForms getSelectedForms() {
    return new TransferForms(formMetadata.stream().filter(formMetadata -> selectedForms.contains(formMetadata.getKey())).collect(toList()));
  }

  public boolean someSelected() {
    return !formMetadata.isEmpty() && !getSelectedForms().isEmpty();
  }

  public boolean allSelected() {
    return !formMetadata.isEmpty() && formMetadata.size() == selectedForms.size();
  }

  public void clear() {
    formMetadata.clear();
    triggerOnChange();
  }

  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  private void triggerOnChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  public boolean isEmpty() {
    return formMetadata.isEmpty();
  }

  public <T> Stream<T> map(Function<FormMetadata, T> mapper) {
    return formMetadata.stream().map(mapper);
  }

  public TransferForms filter(Predicate<FormMetadata> predicate) {
    return new TransferForms(formMetadata.stream().filter(predicate).collect(toList()));
  }

  @Override
  public Iterator<FormMetadata> iterator() {
    return formMetadata.iterator();
  }
}
