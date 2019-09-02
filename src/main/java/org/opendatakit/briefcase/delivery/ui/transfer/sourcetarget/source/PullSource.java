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

package org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public interface PullSource<T> extends SourceOrTarget<T> {
  static PullSource<AggregateServer> aggregate(Http http, Workspace workspace, Consumer<PullSource> consumer) {
    return new Aggregate(http, workspace, server -> http.execute(server.getFormListRequest()), "Must have Data Collector permissions at least", consumer);
  }

  static PullSource<CentralServer> central(Http http, Workspace workspace, Consumer<PullSource> consumer) {
    return new Central(http, workspace, server -> http.execute(server.getCredentialsTestRequest()), consumer);
  }

  static PullSource<Path> collectDir(Workspace workspace, Consumer<PullSource> consumer) {
    return new CollectDir(workspace, consumer);
  }

  static PullSource<FormMetadata> formInComputer(Workspace workspace, Consumer<PullSource> consumer) {
    return new FormInComputer(workspace, consumer);
  }

  List<FormMetadata> getFormList();

  void storeSourcePrefs(BriefcasePreferences prefs, boolean storePasswords);

  JobsRunner pull(TransferForms forms, BriefcasePreferences appPreferences, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort);

  String getDescription();

}

