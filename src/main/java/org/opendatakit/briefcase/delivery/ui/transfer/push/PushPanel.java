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

package org.opendatakit.briefcase.delivery.ui.transfer.push;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.model.Operation.PUSH;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.removeCurrentTarget;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setCurrentTarget;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.GET_CURRENT_TARGET;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getRememberPasswords;

import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.delivery.ui.transfer.TransferPanelForm;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target.TargetPanelValueContainer;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;

public class PushPanel {
  public static final String TAB_NAME = "Push";
  private final TransferPanelForm view;
  private final TransferForms forms;
  private final BriefcasePreferences pushPreferences;
  private final BriefcasePreferences appPreferences;
  private final Analytics analytics;
  private final Container container;
  private JobsRunner pushJobRunner;
  private Optional<TargetPanelValueContainer> target;

  private PushPanel(Container container, TransferPanelForm<TargetPanelValueContainer> view, TransferForms forms, BriefcasePreferences pushPreferences, BriefcasePreferences appPreferences, Analytics analytics) {
    this.container = container;
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.pushPreferences = pushPreferences;
    this.appPreferences = appPreferences;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Push"));

    // Read prefs and load saved remote server if available
    target = container.preferences.query(GET_CURRENT_TARGET).flatMap(view::preloadSourceOrTarget);
    target.ifPresent(source -> updateActionButtons());

    // Register callbacks to view events
    view.onSelect(target -> {
      this.target = Optional.of(target);
      if (container.preferences.query(getRememberPasswords()))
        container.preferences.execute(setCurrentTarget(target.get()));
      updateActionButtons();
    });

    view.onReset(() -> {
      target = Optional.empty();
      view.clearAllStatusLines();
      container.preferences.execute(removeCurrentTarget());
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onAction(() -> {
      view.setWorking();
      view.clearAllStatusLines();
      new Thread(() -> target.ifPresent(s -> {
        pushJobRunner = s.push(forms.getSelectedForms());
        pushJobRunner.waitForCompletion();
      })).start();
    });

    view.onCancel(() -> {
      pushJobRunner.cancel();
      forms.getSelectedForms().forEach(form -> EventBus.publish(new FormStatusEvent(PUSH, form.getKey(), "Cancelled by user")));
      view.unsetWorking();
      view.refresh();
      updateActionButtons();
    });
  }

  public static PushPanel from(Container container, Analytics analytics, BriefcasePreferences appPreferences) {
    TransferForms forms = TransferForms.from(container.formMetadata.fetchAll().collect(toList()));
    return new PushPanel(
        container,
        TransferPanelForm.push(container, forms),
        forms,
        BriefcasePreferences.forClass(PushPanel.class),
        appPreferences,
        analytics
    );
  }

  public JPanel getContainer() {
    return view.container;
  }

  private void updateActionButtons() {
    if (target.isPresent() && forms.someSelected())
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

  private void updateForms() {
    forms.merge(container.formMetadata.fetchAll().collect(toList()));
    view.refresh();
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onFormPulledSuccessfully(PullEvent.Success event) {
    updateForms();
    view.refresh();
  }

  @EventSubscriber(eventClass = PushEvent.Complete.class)
  public void onPushComplete(PushEvent.Complete event) {
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Push", "Transfer", "Success", null);
  }


}
