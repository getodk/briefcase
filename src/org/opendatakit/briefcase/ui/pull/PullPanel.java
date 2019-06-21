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
import static org.opendatakit.briefcase.model.BriefcasePreferences.getStorePasswordsConsentProperty;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.ui.reused.UI.infoMessage;

import java.util.Optional;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.SavePasswordsConsentRevoked;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.reused.transfer.TransferPanelForm;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.source.PullSource;
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
  private JobsRunner pullJobRunner;
  private Optional<PullSource> source;

  private PullPanel(TransferPanelForm<PullSource> view, TransferForms forms, BriefcasePreferences tabPreferences, BriefcasePreferences appPreferences, Analytics analytics) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.tabPreferences = tabPreferences;
    this.appPreferences = appPreferences;
    this.analytics = analytics;
    getContainer().addComponentListener(analytics.buildComponentListener("Pull"));

    // Read prefs and load saved remote server if available
    source = RemoteServer.readFromPrefs(tabPreferences).flatMap(view::preloadOption);
    view.onReady(() -> source.ifPresent(source -> onSource(view, forms, source)));

    // Register callbacks to view events
    view.onSelect(source -> {
      this.source = Optional.of(source);
      source.storeSourcePrefs(tabPreferences, getStorePasswordsConsentProperty());
      onSource(view, forms, source);
    });

    view.onReset(() -> {
      forms.clear();
      view.refresh();
      source = Optional.empty();
      RemoteServer.clearStoredPrefs(tabPreferences);
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onAction(() -> {
      view.setWorking();
      forms.forEach(FormStatus::clearStatusHistory);
      new Thread(() -> source.ifPresent(s -> {
        pullJobRunner = s.pull(forms.getSelectedForms(), appPreferences, tabPreferences);
        pullJobRunner.waitForCompletion();
      })).start();
    });

    view.onCancel(() -> {
      pullJobRunner.cancel();
      forms.getSelectedForms().forEach(form -> {
        form.setStatusString("Cancelled by user");
        EventBus.publish(new FormStatusEvent(form));
      });
      view.unsetWorking();
      view.refresh();
      updateActionButtons();
    });
  }

  public static PullPanel from(Http http, BriefcasePreferences appPreferences, BriefcasePreferences pullPanelPreferences, Analytics analytics) {
    TransferForms forms = TransferForms.empty();
    return new PullPanel(
        TransferPanelForm.pull(http, forms),
        forms,
        pullPanelPreferences,
        appPreferences,
        analytics
    );
  }

  public JPanel getContainer() {
    return view.container;
  }

  private void onSource(TransferPanelForm view, TransferForms forms, PullSource<?> source) {
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

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void onRetrieveAvailableFormsFailedEvent(RetrieveAvailableFormsFailedEvent event) {
    errorMessage("Accessing Server Failed", "Accessing the server failed with error: " + event.getReason());
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    // TODO This should be managed by the Settings vertical. We'll deal with it when we have a central database
    tabPreferences.removeAll(tabPreferences.keys().stream().filter(RemoteServer::isPrefKey).collect(toList()));
    appPreferences.removeAll(appPreferences.keys().stream().filter(RemoteServer::isPrefKey).collect(toList()));
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {
    event.ifRemoteServer((form, server) -> {
      server.clearStoredPrefs(appPreferences, form);
      server.storeInPrefs(appPreferences, form, getStorePasswordsConsentProperty());
    });
    event.lastCursor.ifPresent(cursor -> cursor.storePrefs(event.form, appPreferences));
  }

  @EventSubscriber(eventClass = PullEvent.PullComplete.class)
  public void onPullComplete(PullEvent.PullComplete event) {
    view.unsetWorking();
    updateActionButtons();
    analytics.event("Pull", "Transfer", "Success", null);
  }

  @EventSubscriber(eventClass = PullEvent.CleanAllResumePoints.class)
  public void onCleanAllResumePoints(PullEvent.CleanAllResumePoints e) {
    Cursor.cleanAllPrefs(appPreferences);
    infoMessage("Pull history cleared.");
  }
}
