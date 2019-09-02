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

import javax.swing.JPanel;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.reused.BriefcaseVersionManager;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;

public class SettingsPanel {

  public static final String TAB_NAME = "Settings";
  private final SettingsPanelForm form;

  @SuppressWarnings("checkstyle:Indentation")
  private SettingsPanel(SettingsPanelForm form, BriefcasePreferences appPreferences, Analytics analytics, Http http, BriefcaseVersionManager versionManager, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort, Workspace workspace) {
    this.form = form;

    appPreferences.getMaxHttpConnections().ifPresent(form::setMaxHttpConnections);
    appPreferences.getStartFromLast().ifPresent(form::setResumeLastPull);
    appPreferences.getRememberPasswords().ifPresent(form::setRememberPasswords);
    appPreferences.getSendUsageData().ifPresent(form::setSendUsageData);
    appPreferences.getHttpProxy().ifPresent(httpProxy -> {
      form.enableUseHttpProxy();
      form.setHttpProxy(httpProxy);
      form.updateHttpProxyFields();
    });

    form.onMaxHttpConnectionsChange(appPreferences::setMaxHttpConnections);
    form.onResumeLastPullChange(appPreferences::setStartFromLast);
    form.onRememberPasswordsChange(appPreferences::setRememberPasswords);
    form.onSendUsageDataChange(enabled -> {
      appPreferences.setSendUsage(enabled);
      analytics.enableTracking(enabled, false);
    });
    form.onHttpProxy(proxy -> {
      http.setProxy(proxy);
      appPreferences.setHttpProxy(proxy);
    }, () -> {
      http.unsetProxy();
      appPreferences.unsetHttpProxy();
    });
    form.onReloadCache(() -> {
      formMetadataPort.execute(syncWithFilesAt(workspace.get()));
      submissionMetadataPort.execute(SubmissionMetadataCommands.syncSubmissions(formMetadataPort.fetchAll()));
      infoMessage("Forms successfully reloaded from storage location.");
    });
    form.onCleanAllPullResumePoints(() -> {
      formMetadataPort.execute(cleanAllCursors());
      infoMessage("Pull history cleared.");
    });

    form.setVersion(versionManager.getCurrent());
  }

  public static SettingsPanel from(BriefcasePreferences appPreferences, Analytics analytics, Http http, BriefcaseVersionManager versionManager, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort, Workspace workspace) {
    SettingsPanelForm settingsPanelForm = new SettingsPanelForm();
    return new SettingsPanel(settingsPanelForm, appPreferences, analytics, http, versionManager, formMetadataPort, submissionMetadataPort, workspace);
  }

  public JPanel getContainer() {
    return form.container;
  }

}
