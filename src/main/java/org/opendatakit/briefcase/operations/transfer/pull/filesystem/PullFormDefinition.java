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

package org.opendatakit.briefcase.operations.transfer.pull.filesystem;

import static org.opendatakit.briefcase.operations.transfer.pull.filesystem.FormInstaller.installForm;
import static org.opendatakit.briefcase.operations.transfer.pull.filesystem.PathSourceOrTarget.formDefinitionAt;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;

import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;

public class PullFormDefinition {
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadataPort formMetadataPort;

  public PullFormDefinition(FormMetadataPort formMetadataPort, Consumer<FormStatusEvent> onEventCallback) {
    this.onEventCallback = onEventCallback;
    this.formMetadataPort = formMetadataPort;
  }

  public Job<Void> pull(FormMetadata sourceFormMetadata, FormMetadata targetFormMetadata) {
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(targetFormMetadata.getKey(), onEventCallback);

    return run(rs -> {
      tracker.trackStart();

      installForm(sourceFormMetadata, targetFormMetadata, tracker);

      formMetadataPort.execute(upsert(targetFormMetadata
          .asDefaultFormVersion()
          .withPullSource(formDefinitionAt(sourceFormMetadata.getFormFile()))));

      EventBus.publish(PullEvent.Success.of(targetFormMetadata.getKey()));

      tracker.trackEnd();
    });
  }
}
