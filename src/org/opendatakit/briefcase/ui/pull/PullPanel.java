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

package org.opendatakit.briefcase.ui.pull;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.USERNAME;
import static org.opendatakit.briefcase.model.BriefcasePreferences.getStorePasswordsConsentProperty;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.reused.source.Source;
import org.opendatakit.briefcase.ui.reused.transfer.TransferPanelForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullPanel {
  private static final Logger log = LoggerFactory.getLogger(PullPanel.class);
  public static final String TAB_NAME = "Pull";
  private final TransferPanelForm view;
  private final TransferForms forms;
  private final BriefcasePreferences tabPreferences;
  private final BriefcasePreferences appPreferences;
  private final Analytics analytics;
  private TerminationFuture terminationFuture;
  private Optional<Source<?>> source;

  public PullPanel(TransferPanelForm view, TransferForms forms, BriefcasePreferences tabPreferences, BriefcasePreferences appPreferences, TerminationFuture terminationFuture, Analytics analytics) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.tabPreferences = tabPreferences;
    this.appPreferences = appPreferences;
    this.terminationFuture = terminationFuture;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Pull"));

    // Read prefs and load saved remote server if available
    source = RemoteServer.readPreferences(tabPreferences).flatMap(view::preloadSource);
    source.ifPresent(source -> {
      try {
        forms.load(source.getFormList());
        view.refresh();
        updateActionButtons();
      } catch (HttpException e) {
        log.warn("Unable to get form list from {}: {}", source.getDescription(), e.toString());
        errorMessage("Error preloading forms", "We haven't been able to preload forms using the saved source. Try reloading it or reset it, please.");
      }
    });

    // Register callbacks to view events
    view.onSource(source -> {
      try {
        this.source = Optional.of(source);
        Source.clearAllPreferences(tabPreferences);
        source.storePreferences(tabPreferences, getStorePasswordsConsentProperty());
        forms.load(source.getFormList());
        view.refresh();
        updateActionButtons();
      } catch (HttpException e) {
        errorMessage("Error reloading forms", "We haven't been able to reload forms using the saved source. Try again or reset it, please.");
      }
    });

    view.onReset(() -> {
      forms.clear();
      view.refresh();
      source = Optional.empty();
      Source.clearAllPreferences(tabPreferences);
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onAction(() -> {
      view.setWorking();
      forms.forEach(FormStatus::clearStatusHistory);
      source.ifPresent(s -> s.pull(forms.getSelectedForms(), terminationFuture, appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new), appPreferences.getPullInParallel().orElse(false), false));
    });

    view.onCancel(() -> terminationFuture.markAsCancelled(new PullEvent.Abort("Cancelled by the user")));
  }

  public static PullPanel from(Http http, BriefcasePreferences appPreferences, TerminationFuture terminationFuture, Analytics analytics) {
    TransferForms forms = TransferForms.empty();
    return new PullPanel(
        TransferPanelForm.pull(http, forms),
        forms,
        BriefcasePreferences.forClass(PullPanel.class),
        appPreferences,
        terminationFuture,
        analytics
    );
  }

  public JPanel getContainer() {
    return view.container;
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

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void onRetrieveAvailableFormsFailedEvent(RetrieveAvailableFormsFailedEvent event) {
    ODKOptionPane.showErrorDialog(view.container, "Accessing the server failed with error: " + event.getReason(), "Accessing Server Failed");
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    tabPreferences.remove(AGGREGATE_1_0_URL);
    tabPreferences.remove(USERNAME);
    tabPreferences.remove(PASSWORD);
    appPreferences.removeAll(appPreferences.keys().stream().filter((String key) ->
        key.endsWith("_pull_settings_url")
            || key.endsWith("_pull_settings_username")
            || key.endsWith("_pull_settings_password")
    ).collect(toList()));
  }

  @EventSubscriber(eventClass = PullEvent.Failure.class)
  public void onPullFailure(PullEvent.Failure event) {
    terminationFuture.reset();
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Pull", "Transfer", "Failure", null);
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {
    terminationFuture.reset();
    view.unsetWorking();
    updateActionButtons();
    if (getStorePasswordsConsentProperty()) {
      if (event.transferSettings.isPresent()) {
        event.forms.forEach(form -> {
          appPreferences.put(String.format("%s_pull_settings_url", form.getFormDefinition().getFormId()), event.transferSettings.get().getUrl());
          appPreferences.put(String.format("%s_pull_settings_username", form.getFormDefinition().getFormId()), event.transferSettings.get().getUsername());
          appPreferences.put(String.format("%s_pull_settings_password", form.getFormDefinition().getFormId()), String.valueOf(event.transferSettings.get().getPassword()));
        });
      } else {
        event.forms.forEach(form -> appPreferences.removeAll(
            String.format("%s_pull_settings_url", form.getFormDefinition().getFormId()),
            String.format("%s_pull_settings_username", form.getFormDefinition().getFormId()),
            String.format("%s_pull_settings_password", form.getFormDefinition().getFormId())
        ));
      }
    }
    analytics.event("Pull", "Transfer", "Success", null);
  }
}
