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

import java.util.List;
import java.util.function.Consumer;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.SourceOrTargetPanelValueContainer;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public interface SourcePanelValueContainer extends SourceOrTargetPanelValueContainer {
  static SourcePanelValueContainer aggregate(Container container, Consumer<SourcePanelValueContainer> consumer) {
    return new Aggregate(container, server -> container.http.execute(server.getFormListRequest()), "Must have Data Collector permissions at least", consumer);
  }

  static SourcePanelValueContainer central(Container container, Consumer<SourcePanelValueContainer> consumer) {
    return new Central(container, server -> container.http.execute(server.getCredentialsTestRequest()), consumer);
  }

  static SourcePanelValueContainer collectDir(Container container, Consumer<SourcePanelValueContainer> consumer) {
    return new CollectDir(container, consumer);
  }

  static SourcePanelValueContainer formInComputer(Container container, Consumer<SourcePanelValueContainer> consumer) {
    return new FormInComputer(container, consumer);
  }

  List<FormMetadata> getFormList();

  JobsRunner pull(TransferForms forms, boolean startFromLast);

  String getDescription();

}

