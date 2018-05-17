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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;

public class SourcePanel {
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

    showView.onReset(() -> {
      container.navigateTo(SELECT);
      triggerReset();
    });

    container.navigateTo(SELECT);
  }

  public static SourcePanel pull(Http http, Path briefcaseDir) {
    SourcePanel panel = new SourcePanel(
        new SelectSourceForm("Pull Data From"),
        ShowSourceForm.empty("Pulling data from")
    );
    panel.addSource(new Source.Aggregate(http, panel::triggerOnSource));
    panel.addSource(new Source.CustomDir(panel::triggerOnSource));
    panel.addSource(new Source.FormInComputer(panel::triggerOnSource, briefcaseDir));
    return panel;
  }

  public static SourcePanel push(Http http) {
    SourcePanel panel = new SourcePanel(
        new SelectSourceForm("Push Data To"),
        ShowSourceForm.empty("Pushing data to")
    );
    panel.addSource(new Source.Aggregate(http, panel::triggerOnSource));
    return panel;
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
  public void preload(RemoteServer server) {
    sources.stream()
        .filter(source -> source.accepts(server))
        .findFirst()
        // There's no .peek() method in Optional
        .map(source -> {
          ((Source<RemoteServer>) source).set(server);
          return source;
        })
        .ifPresent(this::triggerOnSource);
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
