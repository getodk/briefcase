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

package org.opendatakit.briefcase.ui.push;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.USERNAME;
import static org.opendatakit.briefcase.model.BriefcasePreferences.getStorePasswordsConsentProperty;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.util.List;
import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.reused.transfer.TransferPanelForm;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.target.PushTarget;
import org.opendatakit.briefcase.util.FormCache;

public class PushPanel {
  public static final String TAB_NAME = "Push";
  private final TransferPanelForm view;
  private final TransferForms forms;
  private final BriefcasePreferences tabPreferences;
  private final BriefcasePreferences appPreferences;
  private final FormCache formCache;
  private final Analytics analytics;
  private JobsRunner pushJobRunner;
  private Optional<PushTarget> target;

  private PushPanel(TransferPanelForm<PushTarget> view, TransferForms forms, BriefcasePreferences tabPreferences, BriefcasePreferences appPreferences, FormCache formCache, Analytics analytics) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.tabPreferences = tabPreferences;
    this.appPreferences = appPreferences;
    this.formCache = formCache;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Push"));

    // Read prefs and load saved remote server if available
    this.target = RemoteServer.readPreferences(tabPreferences).flatMap(view::preloadOption);

    this.target.ifPresent(source -> updateActionButtons());

    // Register callbacks to view events
    view.onSelect(target -> {
      this.target = Optional.of(target);
      PushTarget.clearAllPreferences(tabPreferences);
      target.storePreferences(tabPreferences, getStorePasswordsConsentProperty());
      updateActionButtons();
    });

    view.onReset(() -> {
      target = Optional.empty();
      PushTarget.clearAllPreferences(tabPreferences);
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onAction(() -> {
      view.setWorking();
      forms.forEach(FormStatus::clearStatusHistory);
      new Thread(() -> target.ifPresent(s -> pushJobRunner = s.push(
          forms.getSelectedForms(),
          appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new)
      ))).start();
    });

    view.onCancel(() -> {
      pushJobRunner.cancel();
      forms.getSelectedForms().forEach(form -> {
        form.setStatusString("Cancelled by user");
        EventBus.publish(new FormStatusEvent(form));
      });
      view.unsetWorking();
      updateActionButtons();
    });
  }

  public static PushPanel from(Http http, BriefcasePreferences appPreferences, FormCache formCache, Analytics analytics) {
    TransferForms forms = TransferForms.from(toFormStatuses(formCache.getForms()));
    return new PushPanel(
        TransferPanelForm.push(http, forms),
        forms,
        BriefcasePreferences.forClass(PushPanel.class),
        appPreferences,
        formCache,
        analytics
    );
  }

  private static List<FormStatus> toFormStatuses(List<BriefcaseFormDefinition> formDefs) {
    return formDefs.stream()
        .map(FormStatus::new)
        .collect(toList());
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
    forms.merge(toFormStatuses(formCache.getForms()));
    view.refresh();
  }

  @EventSubscriber(eventClass = CacheUpdateEvent.class)
  public void onCacheUpdateEvent(CacheUpdateEvent event) {
    updateForms();
    view.refresh();
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onCacheUpdateEvent(FormStatusEvent event) {
    view.refresh();
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void onRetrieveAvailableFormsFailedEvent(RetrieveAvailableFormsFailedEvent event) {
    errorMessage("Accessing Server Failed", "Accessing the server failed with error: " + event.getReason());
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    tabPreferences.remove(AGGREGATE_1_0_URL);
    tabPreferences.remove(USERNAME);
    tabPreferences.remove(PASSWORD);
    appPreferences.removeAll(appPreferences.keys().stream().filter((String key) ->
        key.endsWith("_push_settings_url")
            || key.endsWith("_push_settings_username")
            || key.endsWith("_push_settings_password")
    ).collect(toList()));
  }

  @EventSubscriber(eventClass = PushEvent.Failure.class)
  public void onPushFailure(PushEvent.Failure event) {
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Push", "Transfer", "Failure", null);
  }

  @EventSubscriber(eventClass = PushEvent.Success.class)
  public void onPushSuccess(PushEvent.Success event) {
    view.unsetWorking();
    updateActionButtons();
    if (getStorePasswordsConsentProperty() && event.transferSettings.isPresent()) {
      event.forms.forEach(form -> {
        // TODO apply Inversion of Control here to simplify (make the transferSettings object save itself into the prefs)
        appPreferences.put(String.format("%s_push_settings_url", form.getFormDefinition().getFormId()), event.transferSettings.get().getUrl());
        appPreferences.put(String.format("%s_push_settings_username", form.getFormDefinition().getFormId()), event.transferSettings.get().getUsername());
        appPreferences.put(String.format("%s_push_settings_password", form.getFormDefinition().getFormId()), String.valueOf(event.transferSettings.get().getPassword()));
      });
    }
    analytics.event("Push", "Transfer", "Success", null);
  }


}
