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

import static org.opendatakit.briefcase.operations.ClearPreferences.CLEAR_PREFS;
import static org.opendatakit.briefcase.operations.Export.EXPORT_FORM;
import static org.opendatakit.briefcase.operations.ImportFromODK.IMPORT_FROM_ODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.PULL_FORM_FROM_AGGREGATE;

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
    new Cli()
        .register(PULL_FORM_FROM_AGGREGATE)
        .register(IMPORT_FROM_ODK)
        .register(EXPORT_FORM)
        .register(CLEAR_PREFS)
        .otherwise(() -> MainBriefcaseWindow.main(args))
        .run(args);
  }
}
