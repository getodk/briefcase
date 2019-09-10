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

package org.opendatakit.briefcase.delivery.ui.settings;

import static org.opendatakit.briefcase.delivery.ui.reused.UI.infoMessage;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.cleanAllCursors;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.syncWithFilesAt;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.removeSavedServers;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setHttpProxy;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setMaxHttpConnections;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setRememberPasswords;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setStartPullFromLast;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setTrackingConsent;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.unsetHttpProxy;

import javax.swing.JPanel;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands;

public class SettingsPanel {

  public static final String TAB_NAME = "Settings";
  private final SettingsPanelForm form;

  @SuppressWarnings("checkstyle:Indentation")
  private SettingsPanel(Container container, Analytics analytics, SettingsPanelForm form) {
    this.form = form;

    container.preferences.query(PreferenceQueries.getMaxHttpConnections()).ifPresent(form::setMaxHttpConnections);
    container.preferences.query(PreferenceQueries.getStartPullFromLast()).ifPresent(form::setStartPullFromLast);
    container.preferences.query(PreferenceQueries.getRememberPasswords()).ifPresent(form::setRememberPasswords);
    container.preferences.query(PreferenceQueries.getHttpProxy()).ifPresent(proxy -> {
      form.enableUseHttpProxy();
      form.setHttpProxy(proxy);
      form.updateHttpProxyFields();
    });
    form.setSendUsageData(container.preferences.query(PreferenceQueries.getTrackingConsent()));

    form.onMaxHttpConnectionsChange(maxHttpConnections -> container.preferences.execute(setMaxHttpConnections(maxHttpConnections)));
    form.onStartPullFromLastChange(startPullFromLastEnabled -> container.preferences.execute(setStartPullFromLast(startPullFromLastEnabled)));
    form.onRememberPasswordsChange(rememberEnabled -> {
      container.preferences.execute(setRememberPasswords(rememberEnabled));
      if (!rememberEnabled) {
        container.preferences.execute(removeSavedServers());
        container.formMetadata.forgetPullSources();
      }
    });
    form.onSendUsageDataChange(enabled -> {
      container.preferences.execute(setTrackingConsent(enabled));
      analytics.enableTracking(enabled, false);
    });
    form.onHttpProxyChange(proxy -> {
      container.http.setProxy(proxy);
      container.preferences.execute(setHttpProxy(proxy));
    }, () -> {
      container.http.unsetProxy();
      container.preferences.execute(unsetHttpProxy());
    });
    form.onReloadCache(() -> {
      container.formMetadata.execute(syncWithFilesAt(container.workspace.get()));
      container.submissionMetadata.execute(SubmissionMetadataCommands.syncSubmissions(container.formMetadata.fetchAll()));
      infoMessage("Forms successfully reloaded from storage location.");
    });
    form.onCleanAllPullResumePoints(() -> {
      container.formMetadata.execute(cleanAllCursors());
      infoMessage("Pull history cleared.");
    });

    form.setVersion(container.versionManager.getCurrent());
  }

  public static SettingsPanel from(Container container, Analytics analytics) {
    return new SettingsPanel(container, analytics, new SettingsPanelForm());
  }

  public JPanel getContainer() {
    return form.container;
  }

}
