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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;

import java.nio.file.Path;
import java.util.Collections;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.UncheckedFiles;

/**
 * This class has UI/CLI independent methods to install forms into
 * Briefcase's Storage Directory.
 * <p>
 * The goal of this class is to be as decoupled as we can to make
 * testing these business critical operations easier.
 */
public class FormInstaller {
  /**
   * Takes a {@link FormStatus} and installs the form definition file and any
   * related media files into Briefcase's Storage Directory.
   * <p>
   * After installing the form, it updates the form cache.
   * <p>
   * This method won't install any submission.
   */
  public static void install(Path briefcaseDir, FormStatus fs) {
    try {
      installForm(briefcaseDir, fs);
      EventBus.publish(new PullEvent.Success(Collections.singletonList(fs)));
    } catch (BriefcaseException e) {
      EventBus.publish(new PullEvent.Failure());
    }
  }

  private static void installForm(Path briefcaseDir, FormStatus form) {
    // Get the source form definition file
    Path sourceFormFile = getSourceFormFile(form);

    // Get and prepare target form directory inside our Storage Directory
    Path targetFormDir = getTargetFormDir(briefcaseDir, form);
    if (!exists(targetFormDir))
      createDirectories(targetFormDir);

    // Install the form definition file, changing the filename on the process
    Path targetFormFile = targetFormDir.resolve(form.getFormName() + ".xml");
    copy(sourceFormFile, targetFormFile, REPLACE_EXISTING);
    form.setStatusString("Installed form definition file", true);
    EventBus.publish(new FormStatusEvent(form));

    // Check if there is a media directory to install as well
    Path sourceMediaDir = sourceFormFile.resolveSibling(form.getFormName() + "-media");
    Path targetMediaDir = targetFormDir.resolve(form.getFormName() + "-media");
    if (exists(targetMediaDir))
      deleteRecursive(targetMediaDir);
    if (exists(sourceMediaDir))
      walk(sourceMediaDir)
          .forEach(sourcePath -> {
            copy(sourcePath, targetMediaDir.resolve(sourceMediaDir.relativize(sourcePath)));
            form.setStatusString("Installed " + sourcePath.getFileName() + " media file", true);
            form.setStatusString("Installed " + sourcePath.getFileName() + " media file", true);
            EventBus.publish(new FormStatusEvent(form));
          });

    // Create an empty instances directory
    UncheckedFiles.createDirectories(targetFormDir.resolve("instances"));

    form.setStatusString("Success", true);
    EventBus.publish(new FormStatusEvent(form));
  }

  private static Path getTargetFormDir(Path briefcaseDir, FormStatus form) {
    return briefcaseDir.resolve("forms").resolve(form.getFormName());
  }

  private static Path getSourceFormFile(FormStatus form) {
    return ((OdkCollectFormDefinition) form.getFormDefinition()).getFormDefinitionFile().toPath();
  }
}
