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
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.FormStatus;

/**
 * This class represents a set of forms to be pulled/pushed. It manages the
 * selection of those forms, as well as merging changes in the forms cache.
 */
public class TransferForms implements Iterable<FormStatus> {
  private List<FormStatus> forms;
  private Map<String, FormStatus> formsIndex = new HashMap<>();
  private final List<Runnable> onChangeCallbacks = new ArrayList<>();

  private TransferForms(List<FormStatus> forms) {
    this.forms = forms;
    rebuildIndex();
  }

  /**
   * Factory of empty {@link TransferForms} instances
   */
  public static TransferForms empty() {
    return new TransferForms(Collections.emptyList());
  }

  /**
   * Factory of {@link TransferForms} instances that begin with the given
   * list of {@link FormStatus} instances
   */
  public static TransferForms from(List<FormStatus> forms) {
    return new TransferForms(forms);
  }

  private static String getFormId(FormStatus form) {
    return form.getFormDefinition().getFormId();
  }

  public static TransferForms of(FormStatus... forms) {
    return new TransferForms(Arrays.asList(forms));
  }

  /**
   * Replaces the current list of {@link FormStatus} instances with
   * the incoming list.
   */
  public void load(List<FormStatus> forms) {
    this.forms = forms;
    triggerOnChange();
  }

  /**
   * Merges the current list of {@link FormStatus} instances with
   * the incoming list.
   * <p>
   * The merge process only adds new incoming elements and removes
   * those not present in the incoming list.
   * <p>
   * This is done so that any mutation on the current {@link FormStatus}
   * instances isn't lost.
   */
  public void merge(List<FormStatus> incomingForms) {
    List<String> incomingFormIds = incomingForms.stream().map(TransferForms::getFormId).collect(toList());
    List<FormStatus> formsToAdd = incomingForms.stream().filter(form -> !formsIndex.containsKey(getFormId(form))).collect(toList());
    List<FormStatus> formsToRemove = formsIndex.values().stream().filter(form -> !incomingFormIds.contains(getFormId(form))).collect(toList());
    forms.addAll(formsToAdd);
    forms.removeAll(formsToRemove);
    rebuildIndex();
    triggerOnChange();
  }

  /**
   * Returns the number of {@link FormStatus} instances present.
   */
  public int size() {
    return forms.size();
  }

  /**
   * Returns the {@link FormStatus} instance corresponding to the given
   * {@link Integer} index.
   */
  public FormStatus get(int rowIndex) {
    return forms.get(rowIndex);
  }

  /**
   * Marks all present {@link FormStatus} instances as being selected.
   */
  // TODO extract the selection status to this class instead of mutating the FormStatus instances
  public void selectAll() {
    forms.forEach(form -> form.setSelected(true));
    triggerOnChange();
  }

  /**
   * Unmarks any {@link FormStatus} instance selection.
   */
  public void clearAll() {
    forms.forEach(form -> form.setSelected(false));
    triggerOnChange();
  }

  /**
   * Returns a list of selected {@link FormStatus} instances.
   */
  public TransferForms getSelectedForms() {
    return new TransferForms(forms.stream().filter(FormStatus::isSelected).collect(toList()));
  }

  /**
   * Returns true if at least one {@link FormStatus} instance has been
   * marked as selected, false otherwise.
   */
  public boolean someSelected() {
    return !forms.isEmpty() && !getSelectedForms().isEmpty();
  }

  /**
   * Returns true if all {@link FormStatus} instances has been
   * marked as selected, false otherwise.
   */
  public boolean allSelected() {
    return !forms.isEmpty() && forms.stream().allMatch(FormStatus::isSelected);
  }

  /**
   * Empties the {@link FormStatus} list.
   */
  public void clear() {
    forms = Collections.emptyList();
    triggerOnChange();
  }

  private void rebuildIndex() {
    formsIndex = forms.stream().collect(toMap(TransferForms::getFormId, form -> form));
  }

  /**
   * Let's calling sites react to changes on the {@link FormStatus} list.
   * <p>
   * A change can be due to:
   * <ul>
   * <li>New elements added</li>
   * <li>Elements removed</li>
   * <li>Selection of an element changed</li>
   * </ul>
   */
  public void onChange(Runnable callback) {
    onChangeCallbacks.add(callback);
  }

  private void triggerOnChange() {
    onChangeCallbacks.forEach(Runnable::run);
  }

  /**
   * Returns true if the list of {@link FormStatus} is empty, false otherwise
   */
  public boolean isEmpty() {
    return forms.isEmpty();
  }

  public <T> Stream<T> map(Function<FormStatus, T> mapper) {
    return forms.stream().map(mapper);
  }

  public String getLastCursor(FormStatus fs) {
    return "";
  }

  @Override
  public Iterator<FormStatus> iterator() {
    return forms.iterator();
  }
}
