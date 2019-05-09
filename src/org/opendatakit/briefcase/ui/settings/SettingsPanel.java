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

package org.opendatakit.briefcase.ui.settings;

import static org.opendatakit.briefcase.ui.reused.UI.infoMessage;

import java.nio.file.Path;
import javax.swing.JPanel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.util.FormCache;

public class SettingsPanel {

  public static final String TAB_NAME = "Settings";
  private final SettingsPanelForm form;

  @SuppressWarnings("checkstyle:Indentation")
  private SettingsPanel(SettingsPanelForm form, BriefcasePreferences appPreferences, Analytics analytics, FormCache formCache, Http http) {
    this.form = form;

    appPreferences.getBriefcaseDir().ifPresent(path -> form.setStorageLocation(path.getParent()));
    appPreferences.getPullInParallel().ifPresent(form::setPullInParallel);
    appPreferences.getResumeLastPull().ifPresent(form::setResumeLastPull);
    appPreferences.getRememberPasswords().ifPresent(form::setRememberPasswords);
    appPreferences.getSendUsageData().ifPresent(form::setSendUsageData);
    appPreferences.getHttpProxy().ifPresent(httpProxy -> {
      form.enableUseHttpProxy();
      form.setHttpProxy(httpProxy);
      form.updateHttpProxyFields();
    });

    form.onStorageLocation(path -> {
      Path briefcaseDir = BriefcasePreferences.buildBriefcaseDir(path);
      UncheckedFiles.createBriefcaseDir(briefcaseDir);
      formCache.setLocation(briefcaseDir);
      formCache.update();
      appPreferences.setStorageDir(path);
    }, () -> {
      formCache.unsetLocation();
      formCache.update();
      appPreferences.unsetStorageDir();
    });
    form.onPullInParallelChange(appPreferences::setPullInParallel);
    form.onResumeLastPullChange(appPreferences::setResumeLastPull);
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
      formCache.update();
      infoMessage("Forms successfully reloaded from storage location.");
    });
    form.onCleanAllPullResumePoints(() -> EventBus.publish(new PullEvent.CleanAllResumePoints()));
  }

  public static SettingsPanel from(BriefcasePreferences appPreferences, Analytics analytics, FormCache formCache, Http http) {
    SettingsPanelForm settingsPanelForm = new SettingsPanelForm();
    return new SettingsPanel(settingsPanelForm, appPreferences, analytics, formCache, http);
  }

  public JPanel getContainer() {
    return form.container;
  }

}
