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

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.DeferredValue;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.SourceOrTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PullSource<T> extends SourceOrTarget<T> {
  Logger log = LoggerFactory.getLogger(PullSource.class);

  static void clearAllPreferences(BriefcasePreferences prefs) {
    Aggregate.clearPreferences(prefs);
    CustomDir.clearPreferences(prefs);
    FormInComputer.clearPreferences(prefs);
  }

  static PullSource<RemoteServer> aggregate(Http http, Consumer<PullSource> consumer) {
    return new Aggregate(http, server -> server.testPull(http), "Data Viewer", consumer);
  }

  static PullSource<Path> customDir(Consumer<PullSource> consumer) {
    return new CustomDir(consumer);
  }

  static PullSource<FormStatus> formInComputer(Consumer<PullSource> consumer) {
    return new FormInComputer(consumer);
  }

  DeferredValue<List<FormStatus>> getFormList();

  void storePreferences(BriefcasePreferences prefs, boolean storePasswords);

  JobsRunner pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate);

  String getDescription();

}
