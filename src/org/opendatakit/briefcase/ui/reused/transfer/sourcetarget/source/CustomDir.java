/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.source;

import static java.awt.Cursor.getPredefinedCursor;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.awt.Cursor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.DeferredValue;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;

public class CustomDir implements PullSource<Path> {
  private final Consumer<PullSource> consumer;
  private Path path;

  CustomDir(Consumer<PullSource> consumer) {
    this.consumer = consumer;
  }

  private static boolean isValidCustomDir(Path f) {
    return Files.exists(f) && Files.isDirectory(f) && !isUnderBriefcaseFolder(f.toFile()) && Files.exists(f.resolve("forms")) && Files.isDirectory(f.resolve("forms"));
  }

  static void clearPreferences(BriefcasePreferences prefs) {
    // No prefs to clear
  }

  @Override
  public void onSelect(Container container) {
    FileChooser
        .directory(container, Optional.empty())
        .choose()
        // TODO Changing the FileChooser to handle Paths instead of Files would improve this code and it's also coherent with the modernization (use NIO2 API) of this basecode
        .ifPresent(file -> {
          if (isValidCustomDir(file.toPath()))
            set(file.toPath());
          else {
            errorMessage(
                "Wrong directory",
                "The selected directory doesn't look like an ODK Collect storage directory. Please select another directory."
            );
          }
        });
  }

  @Override
  public void set(Path path) {
    this.path = path;
    consumer.accept(this);
  }

  @Override
  public boolean accepts(Object o) {
    return o instanceof Path;
  }

  @Override
  public DeferredValue<List<FormStatus>> getFormList() {
    return DeferredValue.of(() -> FileSystemUtils.getODKFormList(path.toFile()).stream()
        .map(FormStatus::new)
        .collect(toList()));
  }

  @Override
  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    // No prefs to store
  }

  @Override
  public JobsRunner<Void> pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
    return JobsRunner.launchAsync(
        run(jobStatus -> TransferAction.transferODKToBriefcase(briefcaseDir, path.toFile(), jobStatus, forms))
    );
  }

  @Override
  public boolean canBeReloaded() {
    return false;
  }

  @Override
  public String getDescription() {
    return path.toString();
  }

  @Override
  public void decorate(JLabel label) {
    label.setText(getDescription());
    label.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    removeAllMouseListeners(label);
  }

  @Override
  public String toString() {
    return "Collect directory";
  }
}
