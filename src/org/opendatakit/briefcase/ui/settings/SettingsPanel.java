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

import java.nio.file.Path;
import javax.swing.JPanel;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.briefcase.util.NullFormCache;

public class SettingsPanel {
  private static final String README_CONTENTS = "" +
      "This ODK Briefcase storage area retains\n" +
      "all the forms and submissions that have been\n" +
      "gathered into it.\n" +
      "\n" +
      "Users should not navigate into or modify its\n" +
      "contents unless explicitly directed to do so.\n";

  public static final String TAB_NAME = "Settings";
  private final SettingsPanelForm form;

  @SuppressWarnings("checkstyle:Indentation")
  public SettingsPanel(SettingsPanelForm form, BriefcasePreferences appPreferences, Analytics analytics) {
    this.form = form;

    appPreferences.getBriefcaseDir().ifPresent(path -> form.setStorageLocation(path.getParent()));
    appPreferences.getPullInParallel().ifPresent(form::setPullInParallel);
    appPreferences.getRememberPasswords().ifPresent(form::setRememberPasswords);
    appPreferences.getSendUsageData().ifPresent(form::setSendUsageData);
    appPreferences.getHttpProxy().ifPresent(form::setHttpProxy);

    form.onStorageLocation(path -> {
      Path briefcaseDir = BriefcasePreferences.buildBriefcaseDir(path);
      UncheckedFiles.createDirectories(briefcaseDir);
      UncheckedFiles.createDirectories(briefcaseDir.resolve("forms"));
      UncheckedFiles.write(briefcaseDir.resolve("readme.txt"), README_CONTENTS.getBytes());
      FileSystemUtils.setFormCache(FormCache.from(briefcaseDir));
      appPreferences.setStorageDir(path);
    }, () -> {
      FileSystemUtils.setFormCache(new NullFormCache());
      appPreferences.unsetStorageDir();
    });
    form.onPullInParallelChange(appPreferences::setPullInParallel);
    form.onRememberPasswordsChange(appPreferences::setRememberPasswords);
    form.onSendUsageDataChange(enabled -> {
      appPreferences.setSendUsage(enabled);
      analytics.enableTracking(enabled, false);
    });
    form.onHttpProxy(appPreferences::setHttpProxy, appPreferences::unsetHttpProxy);
  }

  public static SettingsPanel from(BriefcasePreferences appPreferences, Analytics analytics) {
    SettingsPanelForm settingsPanelForm = new SettingsPanelForm();
    return new SettingsPanel(settingsPanelForm, appPreferences, analytics);
  }

  public JPanel getContainer() {
    return form.container;
  }

}
