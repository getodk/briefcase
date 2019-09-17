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

package org.opendatakit.briefcase.delivery.ui.transfer.pull;

import static org.opendatakit.briefcase.delivery.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.model.Operation.PULL;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.removeCurrentSource;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setCurrentSource;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.GET_CURRENT_SOURCE;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getRememberPasswords;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getStartPullFromLast;

import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.delivery.ui.transfer.TransferPanelForm;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source.SourcePanelValueContainer;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullPanel {
  private static final Logger log = LoggerFactory.getLogger(PullPanel.class);
  public static final String TAB_NAME = "Pull";
  private final TransferPanelForm view;
  private final TransferForms forms;
  private final Analytics analytics;
  private JobsRunner pullJobRunner;
  private Optional<SourcePanelValueContainer> source;

  private PullPanel(Container container, TransferPanelForm<SourcePanelValueContainer> view, TransferForms forms, Analytics analytics) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Pull"));

    source = container.preferences.query(GET_CURRENT_SOURCE).flatMap(view::preloadSourceOrTarget);
    view.onReady(() -> source.ifPresent(source -> onSource(view, forms, source)));

    // Register callbacks to view events
    view.onSelect(source -> {
      this.source = Optional.of(source);
      if (container.preferences.query(getRememberPasswords()))
        container.preferences.execute(setCurrentSource(source.get()));
      onSource(view, forms, source);
    });

    view.onReset(() -> {
      forms.clear();
      view.clearAllStatusLines();
      view.refresh();
      source = Optional.empty();
      container.preferences.execute(removeCurrentSource());
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onAction(() -> {
      view.setWorking();
      view.clearAllStatusLines();
      new Thread(() -> source.ifPresent(s -> {
        pullJobRunner = s.pull(forms.getSelectedForms(), container.preferences.query(getStartPullFromLast()));
        pullJobRunner.waitForCompletion();
      })).start();
    });

    view.onCancel(() -> {
      pullJobRunner.cancel();
      forms.getSelectedForms().forEach(formMetadata -> EventBus.publish(new FormStatusEvent(PULL, formMetadata.getKey(), "Cancelled by user")));
      view.unsetWorking();
      view.refresh();
      updateActionButtons();
    });
  }

  public static PullPanel from(Container container, Analytics analytics) {
    TransferForms forms = TransferForms.empty();
    return new PullPanel(
        container,
        TransferPanelForm.pull(container, forms),
        forms,
        analytics
    );
  }

  public JPanel getContainer() {
    return view.container;
  }

  private void onSource(TransferPanelForm view, TransferForms forms, SourcePanelValueContainer source) {
    JobsRunner.launchAsync(
        run(__ -> {
          forms.load(source.getFormList());
          view.refresh();
          updateActionButtons();
        }),
        cause -> {
          log.warn("Unable to load form list from {}", source.getDescription(), cause);
          errorMessage("Error Loading Forms", "Briefcase wasn't able to load forms using the configured source. Try Reload or Reset.");
        }
    );
  }

  private void updateActionButtons() {
    if (source.isPresent() && forms.someSelected())
      view.enableAction();
    else
      view.disableAction();
    if (forms.isEmpty())
      view.disableSelectAll();
    else
      view.enableSelectAll();
    if (forms.allSelected())
      view.showClearAll();
    else
      view.showSelectAll();
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    view.refresh();
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {

    event.ifRemoteServer((formKey, server) -> {
      // TODO Deal with this in the PullFromXYZ class
    });
  }

  @EventSubscriber(eventClass = PullEvent.PullComplete.class)
  public void onPullComplete(PullEvent.PullComplete event) {
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Pull", "Transfer", "Success", null);
  }
}
