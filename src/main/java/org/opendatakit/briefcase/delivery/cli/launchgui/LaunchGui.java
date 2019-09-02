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
package org.opendatakit.briefcase.delivery.cli.launchgui;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.opendatakit.briefcase.delivery.cli.Common.WORKSPACE_LOCATION;

import java.util.Objects;
import org.opendatakit.briefcase.delivery.ui.MainBriefcaseWindow;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;


public class LaunchGui {
  private static final Param<Void> LAUNCH_GUI_FLAG = Param.flag("gui", "gui", "Launch GUI");

  public static Operation create(Workspace workspace, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort) {
    return
        Operation.of(
            LAUNCH_GUI_FLAG,
            __ -> launchGui(workspace, formMetadataPort, submissionMetadataPort),
            emptyList(),
            singletonList(WORKSPACE_LOCATION)
        ).before(args -> {
          if (args.isEmpty(WORKSPACE_LOCATION))
            try {
              WorkspaceLocationDialogForm dialog = new WorkspaceLocationDialogForm(workspaceLocation -> args.set(
                  WORKSPACE_LOCATION,
                  workspaceLocation
                      .map(Objects::toString)
                      .orElseThrow(() -> new BriefcaseException("No workspace location has been chosen or set via CLI args"))
              ));
              dialog.setSize(400, 300);
              dialog.setLocationRelativeTo(null);
              dialog.pack();
              dialog.setVisible(true);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
        });
  }

  private static void launchGui(Workspace workspace, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort) {
    MainBriefcaseWindow.launchGUI(workspace, formMetadataPort, submissionMetadataPort);
  }
}
