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
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.getStorePasswordsConsentProperty;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.UPLOAD;

import java.util.List;
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
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.push.PushForms;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.ui.push.components.PushFormsTable;
import org.opendatakit.briefcase.ui.push.components.PushFormsTableView;
import org.opendatakit.briefcase.ui.push.components.PushFormsTableViewModel;
import org.opendatakit.briefcase.ui.reused.source.Source;
import org.opendatakit.briefcase.ui.reused.source.SourcePanel;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class PushPanel {
  public static final String TAB_NAME = "Push";
  private final PushPanelForm view;
  private final PushForms forms;
  private final BriefcasePreferences tabPreferences;
  private final BriefcasePreferences appPreferences;
  private TerminationFuture terminationFuture;
  private Optional<Source> source = Optional.empty();

  public PushPanel(PushPanelForm view, PushForms forms, BriefcasePreferences tabPreferences, BriefcasePreferences appPreferences, TerminationFuture terminationFuture) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.tabPreferences = tabPreferences;
    this.appPreferences = appPreferences;
    this.terminationFuture = terminationFuture;

    // Register callbacks to view events
    view.onSource(source -> {
      this.source = Optional.of(source);
      Source.clearAllPreferences(tabPreferences);
      source.storePreferences(tabPreferences, getStorePasswordsConsentProperty());
      updateActionButtons();
    });

    view.onReset(() -> {
      source = Optional.empty();
      Source.clearAllPreferences(tabPreferences);
      updateActionButtons();
    });

    view.onChange(this::updateActionButtons);

    view.onPush(() -> {
      view.setPushing();
      forms.forEach(FormStatus::clearStatusHistory);
      source.ifPresent(s -> s.push(forms.getSelectedForms(), terminationFuture));
    });

    view.onCancel(() -> terminationFuture.markAsCancelled(new TransferAbortEvent("Cancelled by the user")));

    // Read prefs and load saved remote server if available
    RemoteServer.readPreferences(tabPreferences).ifPresent(view::preloadSource);

    updateActionButtons();
  }

  public static PushPanel from(Http http, BriefcasePreferences appPreferences, TerminationFuture terminationFuture) {
    PushForms pushForms = new PushForms(getFormsFromStorage());
    PushFormsTableViewModel pushFormsTableViewModel = new PushFormsTableViewModel(pushForms);
    PushFormsTableView pushFormsTableView = new PushFormsTableView(pushFormsTableViewModel);
    PushFormsTable pushFormsTable = new PushFormsTable(pushForms, pushFormsTableView, pushFormsTableViewModel);
    SourcePanel sourcePanel = SourcePanel.push(http);
    return new PushPanel(
        new PushPanelForm(sourcePanel, pushFormsTable),
        pushForms,
        BriefcasePreferences.forClass(PushPanel.class),
        appPreferences,
        terminationFuture
    );
  }

  private static List<FormStatus> getFormsFromStorage() {
    return FileSystemUtils.formCache.getForms().stream()
        .map(formDefinition -> new FormStatus(UPLOAD, formDefinition))
        .collect(toList());
  }

  public JPanel getContainer() {
    return view.container;
  }

  private void updateActionButtons() {
    if (source.isPresent() && forms.someSelected())
      view.enablePush();
    else
      view.disablePush();
    if (forms.isEmpty())
      view.disableSelectAll();
    else
      view.enableSelectAll();
    if (forms.allSelected())
      view.showClearAll();
    else
      view.showSelectAll();
  }

  public void updateForms() {
    forms.merge(getFormsFromStorage());
    view.refresh();
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    updateForms();
    view.refresh();
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void onRetrieveAvailableFormsFailedEvent(RetrieveAvailableFormsFailedEvent event) {
    ODKOptionPane.showErrorDialog(view.container, "Accessing the server failed with error: " + event.getReason(), "Accessing Server Failed");
  }

  @EventSubscriber(eventClass = SavePasswordsConsentRevoked.class)
  public void onSavePasswordsConsentRevoked(SavePasswordsConsentRevoked event) {
    tabPreferences.remove(PASSWORD);
    appPreferences.removeAll(appPreferences.keys().stream().filter((String key) ->
        key.endsWith("_push_settings_url")
            || key.endsWith("_push_settings_username")
            || key.endsWith("_push_settings_password")
    ).collect(toList()));
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void onTransferFailedEvent(TransferFailedEvent event) {
    terminationFuture.reset();
    view.unsetPushing();
    updateActionButtons();
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void onTransferSucceededEvent(TransferSucceededEvent event) {
    terminationFuture.reset();
    view.unsetPushing();
    updateActionButtons();
    if (getStorePasswordsConsentProperty() && event.transferSettings.isPresent()) {
      event.formsToTransfer.forEach(form -> {
        appPreferences.put(String.format("%s_push_settings_url", form.getFormDefinition().getFormId()), event.transferSettings.get().getUrl());
        appPreferences.put(String.format("%s_push_settings_username", form.getFormDefinition().getFormId()), event.transferSettings.get().getUsername());
        appPreferences.put(String.format("%s_push_settings_password", form.getFormDefinition().getFormId()), String.valueOf(event.transferSettings.get().getPassword()));
      });
    }
  }


}
