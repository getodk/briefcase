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

package org.opendatakit.briefcase.ui.reused.source;

import static org.opendatakit.briefcase.ui.reused.source.SourcePanel.View.SELECT;
import static org.opendatakit.briefcase.ui.reused.source.SourcePanel.View.SHOW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourcePanel {
  private static final Logger log = LoggerFactory.getLogger(SourcePanel.class);
  private final SourcePanelForm container = new SourcePanelForm();
  private final List<Source> sources = new ArrayList<>();
  private final List<Consumer<Source<?>>> onSourceCallbacks = new ArrayList<>();
  private final List<Runnable> onResetCallbacks = new ArrayList<>();
  private final ShowSourceForm showView;
  private final SelectSourceForm selectView;

  private SourcePanel(SelectSourceForm selectView, ShowSourceForm showView) {
    container.addForm(SELECT, selectView.container);
    container.addForm(SHOW, showView.container);

    this.showView = showView;
    this.selectView = selectView;

    showView.onReset(this::reset);
    showView.onReload(this::reload);

    container.navigateTo(SELECT);
  }

  public static SourcePanel pull(Http http) {
    SourcePanel panel = new SourcePanel(
        new SelectSourceForm("Pull from"),
        ShowSourceForm.pull("Pulling from")
    );
    panel.addSource(Source.aggregatePull(http, panel::triggerOnSource));
    panel.addSource(Source.customDir(panel::triggerOnSource));
    panel.addSource(Source.formInComputer(panel::triggerOnSource));
    return panel;
  }

  public static SourcePanel push(Http http) {
    SourcePanel panel = new SourcePanel(
        new SelectSourceForm("Push to"),
        ShowSourceForm.push("Pushing to")
    );
    panel.addSource(Source.aggregatePush(http, panel::triggerOnSource));
    return panel;
  }

  private void reset() {
    container.navigateTo(SELECT);
    triggerReset();
  }

  private void reload() {
    triggerOnSource(selectView.getSelectedSource().get());
  }

  private void addSource(Source source) {
    sources.add(source);
    selectView.addSource(source);
  }

  public void onReset(Runnable runnable) {
    onResetCallbacks.add(runnable);
  }

  public void onSource(Consumer<Source<?>> callback) {
    onSourceCallbacks.add(callback);
  }

  public SourcePanelForm getContainer() {
    return container;
  }

  private void triggerReset() {
    onResetCallbacks.forEach(Runnable::run);
  }

  private void triggerOnSource(Source source) {
    onSourceCallbacks.forEach(callback -> callback.accept(source));
    showView.showSource(source);
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
  public Optional<Source<?>> preload(RemoteServer server) {
    try {
      Source source = getSource(server);
      source.set(server);
      triggerOnSource(source);
      return Optional.of(source);
    } catch (HttpException e) {
      log.warn("Can't preload source. Resetting view", e);
      reset();
      return Optional.empty();
    }
  }

  private Source getSource(RemoteServer server) {
    return sources.stream()
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
