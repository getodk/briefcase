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
package org.opendatakit.briefcase.delivery.cli;

import static java.util.Collections.singletonList;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.STORAGE_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.FormInstaller;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PullFromCollectDir;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullFromCollect {
  private static final Logger log = LoggerFactory.getLogger(PullFromCollect.class);
  private static final Param<Void> IMPORT = Param.flag("pc", "pull_collect", "Pull from Collect");
  private static final Param<Path> ODK_DIR = Param.arg("od", "odk_directory", "ODK directory", Paths::get);

  public static Operation create(FormMetadataPort formMetadataPort) {
    return Operation.of(
        IMPORT,
        args -> importODK(formMetadataPort, args),
        Arrays.asList(STORAGE_DIR, ODK_DIR),
        singletonList(FORM_ID)
    );
  }

  private static void importODK(FormMetadataPort formMetadataPort, Args args) {
    String storageDir = args.get(STORAGE_DIR);
    Path odkDir = args.get(ODK_DIR);
    Optional<String> formId = args.getOptional(FORM_ID);

    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);

    List<FormMetadata> formMetadataList = FormInstaller.scanCollectFormsAt(odkDir);

    TransferForms forms = TransferForms.from(formMetadataList)
        .filter(formMetadata -> formId.map(id -> formMetadata.getKey().getId().equals(id)).orElse(true));

    forms.selectAll();

    if (formId.isPresent() && forms.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    PullFromCollectDir pullOp = new PullFromCollectDir(formMetadataPort, PullFromCollect::onEvent);
    JobsRunner.launchAsync(
        forms.map(formMetadata -> pullOp.pull(
            formMetadata,
            formMetadata.withFormFile(formMetadata.buildFormFile(briefcaseDir))
        )),
        PullFromCollect::onError
    ).waitForCompletion();

  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
    // The tracker already logs normal events
  }

  private static void onError(Throwable e) {
    System.err.println("Error pulling a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error pulling a form", e);
  }
}
