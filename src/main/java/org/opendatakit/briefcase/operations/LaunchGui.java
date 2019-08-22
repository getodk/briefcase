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
package org.opendatakit.briefcase.operations;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.opendatakit.briefcase.operations.Common.WORKSPACE_LOCATION;

import java.nio.file.Path;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.ui.MainBriefcaseWindow;
import org.opendatakit.briefcase.util.FormCache;


public class LaunchGui {
  private static final Param<Void> LAUNCH_GUI_FLAG = Param.flag("gui", "gui", "Launch GUI");

  public static Operation create(FormMetadataPort formMetadataPort) {
    return
        Operation.of(
            LAUNCH_GUI_FLAG,
            args -> launchGui(formMetadataPort, args),
            emptyList(),
            singletonList(WORKSPACE_LOCATION)
        );
  }

  private static void launchGui(FormMetadataPort formMetadataPort, Args args) {
    Path workspaceLocation = args.getOptional(WORKSPACE_LOCATION)
        .orElseThrow(() -> new BriefcaseException("" +
            "Choosing a workspace location with the GUI " +
            "hasn't been implemented yet. Please, use " +
            "the " + WORKSPACE_LOCATION.getShortCode()));

    FormCache formCache = FormCache.from(workspaceLocation);
    formCache.update();
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    appPreferences.setStorageDir(workspaceLocation);

    MainBriefcaseWindow.launchGUI(formMetadataPort);
  }
}
