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

package org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source.SourcePanelValueContainer;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target.TargetPanelValueContainer;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceOrTargetPanel<T extends SourceOrTargetPanelValueContainer> {
  private static final Logger log = LoggerFactory.getLogger(SourceOrTargetPanel.class);
  private final SourceOrTargetPanelForm container = new SourceOrTargetPanelForm();
  private final List<T> valueContainers = new ArrayList<>();
  private final List<Consumer<T>> onSelectCallbacks = new ArrayList<>();
  private final List<Runnable> onResetCallbacks = new ArrayList<>();
  private final ShowSourceOrTargetForm<T> showView;
  private final SelectSourceOrTargetForm<T> selectView;

  private SourceOrTargetPanel(SelectSourceOrTargetForm<T> selectView, ShowSourceOrTargetForm<T> showView) {
    container.addForm(View.SELECT, selectView.container);
    container.addForm(View.SHOW, showView.container);

    this.showView = showView;
    this.selectView = selectView;

    showView.onReset(this::reset);
    showView.onReload(this::reload);

    container.navigateTo(View.SELECT);
  }

  public static SourceOrTargetPanel<SourcePanelValueContainer> pull(Container container) {
    SourceOrTargetPanel<SourcePanelValueContainer> panel = new SourceOrTargetPanel<>(
        SelectSourceOrTargetForm.pull(),
        ShowSourceOrTargetForm.pull()
    );
    panel.addOption(SourcePanelValueContainer.aggregate(container, panel::triggerOnSelect));
    panel.addOption(SourcePanelValueContainer.central(container, panel::triggerOnSelect));
    panel.addOption(SourcePanelValueContainer.collectDir(container, panel::triggerOnSelect));
    panel.addOption(SourcePanelValueContainer.formInComputer(container, panel::triggerOnSelect));
    return panel;
  }

  public static SourceOrTargetPanel<TargetPanelValueContainer> push(Container container) {
    SourceOrTargetPanel<TargetPanelValueContainer> panel = new SourceOrTargetPanel<>(
        SelectSourceOrTargetForm.push(),
        ShowSourceOrTargetForm.push()
    );
    panel.addOption(TargetPanelValueContainer.aggregate(container, panel::triggerOnSelect));
    panel.addOption(TargetPanelValueContainer.central(container, panel::triggerOnSelect));
    return panel;
  }

  private void reset() {
    container.navigateTo(View.SELECT);
    triggerReset();
  }

  private void reload() {
    triggerOnSelect(selectView.getSelectedOption().orElseThrow(BriefcaseException::new));
  }

  private void addOption(T option) {
    valueContainers.add(option);
    selectView.addOption(option);
  }

  public void onReset(Runnable runnable) {
    onResetCallbacks.add(runnable);
  }

  public void onSelect(Consumer<T> callback) {
    onSelectCallbacks.add(callback);
  }

  public SourceOrTargetPanelForm getContainer() {
    return container;
  }

  private void triggerReset() {
    onResetCallbacks.forEach(Runnable::run);
  }

  private void triggerOnSelect(T selectedOption) {
    onSelectCallbacks.forEach(callback -> callback.accept(selectedOption));
    showView.showSelectedOption(selectedOption);
    container.navigateTo(View.SHOW);
  }

  public void disableInteraction() {
    showView.setEnabled(false);
    selectView.setEnabled(false);
  }

  public void enableInteraction() {
    showView.setEnabled(true);
    selectView.setEnabled(true);
  }

  public Optional<T> preloadSourceOrTarget(SourceOrTarget sourceOrTarget) {
    try {
      T valueContainer = getValueContainer(sourceOrTarget);
      valueContainer.set(sourceOrTarget);
      triggerOnSelect(valueContainer);
      return Optional.of(valueContainer);
    } catch (HttpException e) {
      log.warn("Can't preload option. Resetting view", e);
      reset();
      return Optional.empty();
    }
  }

  private T getValueContainer(SourceOrTarget value) {
    return valueContainers.stream()
        .filter(s -> s.getType() == value.getType())
        .findFirst()
        .orElseThrow(BriefcaseException::new);
  }

  enum View {
    SELECT, SHOW
  }

  /**
   * We need this method here for IntelliJ's GUI Designer only
   */
  @SuppressWarnings("checkstyle:MethodName")
  public JPanel $$$getRootComponent$$$() {
    return container.container;
  }
}
