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

import static org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTargetPanel.View.SELECT;
import static org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTargetPanel.View.SHOW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source.PullSource;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target.PushTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceOrTargetPanel<T extends SourceOrTarget> {
  private static final Logger log = LoggerFactory.getLogger(SourceOrTargetPanel.class);
  private final SourceOrTargetPanelForm container = new SourceOrTargetPanelForm();
  private final List<T> options = new ArrayList<>();
  private final List<Consumer<T>> onSelectCallbacks = new ArrayList<>();
  private final List<Runnable> onResetCallbacks = new ArrayList<>();
  private final ShowSourceOrTargetForm<T> showView;
  private final SelectSourceOrTargetForm<T> selectView;

  private SourceOrTargetPanel(SelectSourceOrTargetForm<T> selectView, ShowSourceOrTargetForm<T> showView) {
    container.addForm(SELECT, selectView.container);
    container.addForm(SHOW, showView.container);

    this.showView = showView;
    this.selectView = selectView;

    showView.onReset(this::reset);
    showView.onReload(this::reload);

    container.navigateTo(SELECT);
  }

  public static SourceOrTargetPanel<PullSource> pull(Http http, Path briefcaseDir) {
    SourceOrTargetPanel<PullSource> panel = new SourceOrTargetPanel<>(
        SelectSourceOrTargetForm.pull(),
        ShowSourceOrTargetForm.pull()
    );
    panel.addOption(PullSource.aggregate(http, briefcaseDir, panel::triggerOnSelect));
    panel.addOption(PullSource.central(http, briefcaseDir, panel::triggerOnSelect));
    panel.addOption(PullSource.collectDir(briefcaseDir, panel::triggerOnSelect));
    panel.addOption(PullSource.formInComputer(briefcaseDir, panel::triggerOnSelect));
    return panel;
  }

  public static SourceOrTargetPanel<PushTarget> push(Http http) {
    SourceOrTargetPanel<PushTarget> panel = new SourceOrTargetPanel<>(
        SelectSourceOrTargetForm.push(),
        ShowSourceOrTargetForm.push()
    );
    panel.addOption(PushTarget.aggregate(http, panel::triggerOnSelect));
    panel.addOption(PushTarget.central(http, panel::triggerOnSelect));
    return panel;
  }

  private void reset() {
    container.navigateTo(SELECT);
    triggerReset();
  }

  private void reload() {
    triggerOnSelect(selectView.getSelectedOption().orElseThrow(BriefcaseException::new));
  }

  private void addOption(T option) {
    options.add(option);
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
    container.navigateTo(SHOW);
  }

  public void disableInteraction() {
    showView.setEnabled(false);
    selectView.setEnabled(false);
  }

  public void enableInteraction() {
    showView.setEnabled(true);
    selectView.setEnabled(true);
  }

  @SuppressWarnings("unchecked")
  public Optional<T> preloadOption(RemoteServer server) {
    try {
      T option = getOption(server);
      option.set(server);
      triggerOnSelect(option);
      return Optional.of(option);
    } catch (HttpException e) {
      log.warn("Can't preload option. Resetting view", e);
      reset();
      return Optional.empty();
    }
  }

  private T getOption(RemoteServer server) {
    return options.stream()
        .filter(s -> s.accepts(server))
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
