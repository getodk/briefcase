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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.target;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.SourceOrTarget;

public interface PushTarget<T> extends SourceOrTarget<T> {
  static void clearSourcePrefs(BriefcasePreferences prefs) {
    AggregateServer.clearStoredPrefs(prefs);
    CentralServer.clearStoredPrefs(prefs);
  }

  static PushTarget<AggregateServer> aggregate(Http http, Consumer<PushTarget> consumer) {
    return new Aggregate(http, server -> http.execute(server.getPushFormPreflightRequest()), "Must have Form Manager permissions", consumer);
  }

  static PushTarget<CentralServer> central(Http http, Consumer<PushTarget> consumer) {
    return new Central(http, server -> http.execute(server.getCredentialsTestRequest()), consumer);
  }

  void storeTargetPrefs(BriefcasePreferences prefs, boolean storePasswords);

  JobsRunner push(TransferForms forms, Path briefcaseDir);

  String getDescription();

}

