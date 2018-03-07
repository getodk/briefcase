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

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line interface contributed by Nafundi
 *
 * @author chartung@nafundi.com
 */
public class BriefcaseCLI {

  private CommandLine mCommandline;

  private static final Logger log = LoggerFactory.getLogger(BaseFormParserForJavaRosa.class);

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

    try {
      if (odkDir != null)
        importODK(storageDir, Paths.get(odkDir));

      if (odkDir == null && server != null)
        pullFormFromAggregate(storageDir, formid, username, password, server);

      if (exportPath != null)
        export(
            storageDir,
            formid,
            Paths.get(exportPath),
            fileName,
            exportMedia,
            overwrite,
            Optional.ofNullable(startDateString).map(s -> LocalDate.parse(s.replaceAll("/", "-"))),
            Optional.ofNullable(endDateString).map(s -> LocalDate.parse(s.replaceAll("/", "-"))),
            Optional.ofNullable(pemKeyFile).map(Paths::get)
        );
    } catch (BriefcaseException e) {
      System.err.println("Error: " + e.message);
      log.error("Error", e);
      System.exit(1);
    } catch (Throwable t) {
      System.err.println("Unexpected error. Please review the logs and contact maintainers on the following URLs:");
      System.err.println("\thttps://opendatakit.slack.com/messages/C374LNDK9/");
      System.err.println("\thttps://forum.opendatakit.org/c/support");
      log.error("Error", t);
      System.exit(1);
    }
  }
}
