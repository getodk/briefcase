/*
 * Copyright (C) 2014 University of Washington.
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


package org.opendatakit.briefcase.ui;

import static org.opendatakit.briefcase.operations.Export.export;
import static org.opendatakit.briefcase.operations.ImportFromODK.importODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.pullFormFromAggregate;

import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.operations.Export;

/**
 * Command line interface contributed by Nafundi
 *
 * @author chartung@nafundi.com
 */
public class BriefcaseCLI {

  private CommandLine mCommandline;

  private static final Log log = LogFactory.getLog(BaseFormParserForJavaRosa.class);

  public BriefcaseCLI(CommandLine cl) {
    mCommandline = cl;
  }

  public void run() {
    String username = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_USERNAME);
    String password = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_PASSWORD);
    String server = mCommandline.getOptionValue(MainBriefcaseWindow.AGGREGATE_URL);
    String formid = mCommandline.getOptionValue(MainBriefcaseWindow.FORM_ID);
    String storageDir = mCommandline.getOptionValue(MainBriefcaseWindow.STORAGE_DIRECTORY);
    String fileName = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_FILENAME);
    String exportPath = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_DIRECTORY);
    String startDateString = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_START_DATE);
    String endDateString = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_END_DATE);
    // note that we invert incoming value
    boolean exportMedia = !mCommandline.hasOption(MainBriefcaseWindow.EXCLUDE_MEDIA_EXPORT);
    boolean overwrite = mCommandline.hasOption(MainBriefcaseWindow.OVERWRITE_CSV_EXPORT);
    String odkDir = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_DIR);
    String pemKeyFile = mCommandline.getOptionValue(MainBriefcaseWindow.PEM_FILE);

    if (odkDir != null) {
      importODK(storageDir, odkDir);
    } else if (server != null) {
      pullFormFromAggregate(storageDir, formid, username, password, server);
    }

    if (exportPath != null) {
      export(
          storageDir,
          formid,
          exportPath,
          Optional.ofNullable(startDateString).map(Export::toDate).orElse(null),
          Optional.ofNullable(endDateString).map(Export::toDate).orElse(null),
          Optional.ofNullable(pemKeyFile)
      );
    }

  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void progress(ExportProgressEvent event) {
    log.info(event.getText());
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void failedCompletion(ExportFailedEvent event) {
    log.error("Failed.");
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    log.error("Transfer Failed");
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void successfulCompletion(ExportSucceededEvent event) {
    log.info("Succeeded.");
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    log.info("Transfer Succeeded");
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void updateDetailedStatus(FormStatusEvent fse) {
    log.info(fse.getStatusString());
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void formsAvailableFromServer(RetrieveAvailableFormsFailedEvent event) {
    log.error("Accessing the server failed with error: " + event.getReason());
  }

}
