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
package org.opendatakit.briefcase;

import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_DSN;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_ENABLED;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.operations.ClearPreferences.CLEAR_PREFS;
import static org.opendatakit.briefcase.operations.Export.EXPORT_FORM;
import static org.opendatakit.briefcase.operations.ImportFromODK.IMPORT_FROM_ODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.PULL_FORM_FROM_AGGREGATE;
import static org.opendatakit.briefcase.operations.PushFormToAggregate.PUSH_FORM_TO_AGGREGATE;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.getOsName;

import io.sentry.Sentry;
import org.opendatakit.briefcase.ui.MainBriefcaseWindow;
import org.opendatakit.common.cli.Cli;

/**
 * Main launcher for Briefcase
 * <p>
 * It leverages the command-line {@link Cli} adapter to define operations and run
 * Briefcase with some command-line args
 */
public class Launcher {
  public static void main(String[] args) {
    if (SENTRY_ENABLED)
      Sentry.init(String.format(
          "%s?release=%s&stacktrace.app.packages=org.opendatakit&tags=os:%s,jvm:%s",
          SENTRY_DSN,
          VERSION,
          getOsName(),
          System.getProperty("java.version")
      ));

    new Cli()
        .register(PULL_FORM_FROM_AGGREGATE)
        .register(PUSH_FORM_TO_AGGREGATE)
        .register(IMPORT_FROM_ODK)
        .register(EXPORT_FORM)
        .register(CLEAR_PREFS)
        .otherwise(() -> MainBriefcaseWindow.main(args))
        .run(args);
  }
}
