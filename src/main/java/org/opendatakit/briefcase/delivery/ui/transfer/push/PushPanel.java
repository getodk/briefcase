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
import static org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences.USERNAME;
import static org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences.getStorePasswordsConsentProperty;

import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.delivery.ui.transfer.TransferPanelForm;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.target.PushTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.operations.transfer.push.PushEvent;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.preferences.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer;

public class PushPanel {
  public static final String TAB_NAME = "Push";
  private final TransferPanelForm view;
  private final TransferForms forms;
  private final BriefcasePreferences pushPreferences;
  private final BriefcasePreferences appPreferences;
  private final Analytics analytics;
  private final Workspace workspace;
  private JobsRunner pushJobRunner;
  private Optional<PushTarget> target;

  private PushPanel(Workspace workspace, TransferPanelForm<PushTarget> view, TransferForms forms, BriefcasePreferences pushPreferences, BriefcasePreferences appPreferences, Analytics analytics) {
    this.workspace = workspace;
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.pushPreferences = pushPreferences;
    this.appPreferences = appPreferences;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Push"));

    // Read prefs and load saved remote server if available
    this.target = RemoteServer.readFromPrefs(pushPreferences).flatMap(view::preloadOption);

    this.target.ifPresent(source -> updateActionButtons());

    // Register callbacks to view events
    view.onSelect(target -> {
      this.target = Optional.of(target);
      PushTarget.clearSourcePrefs(pushPreferences);
      target.storeTargetPrefs(pushPreferences, getStorePasswordsConsentProperty());
      updateActionButtons();
    });

    view.onReset(() -> {
      target = Optional.empty();
      view.clearAllStatusLines();
      PushTarget.clearSourcePrefs(pushPreferences);
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

  public static PushPanel from(Workspace workspace, Analytics analytics, BriefcasePreferences appPreferences) {
    TransferForms forms = TransferForms.from(workspace.formMetadata.fetchAll().collect(toList()));
    return new PushPanel(
        workspace,
        TransferPanelForm.push(workspace, forms),
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
    forms.merge(workspace.formMetadata.fetchAll().collect(toList()));
    view.refresh();
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onFormPulledSuccessfully(PullEvent.Success event) {
    updateForms();
    view.refresh();
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    pushPreferences.remove(AGGREGATE_1_0_URL);
    pushPreferences.remove(USERNAME);
    pushPreferences.remove(PASSWORD);
    appPreferences.removeAll(appPreferences.keys().stream().filter((String key) ->
        key.endsWith("_push_settings_url")
            || key.endsWith("_push_settings_username")
            || key.endsWith("_push_settings_password")
    ).collect(toList()));
  }

  @EventSubscriber(eventClass = PushEvent.Complete.class)
  public void onPushComplete(PushEvent.Complete event) {
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Push", "Transfer", "Success", null);
  }


}
