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
package org.opendatakit.briefcase.operations;

import java.util.List;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.ui.PullTransferPanel;
import org.opendatakit.briefcase.ui.PushTransferPanel;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class CleanPreferences {

  private static Param<Void> CLEAN = Param.flag("c", "clean-prefs", "Clean saved preferences");

  public static Operation CLEAN_PREFS = Operation.of(CLEAN, __ -> clean());

  private static void clean() {
    flush(BriefcasePreferences.appScoped());
    Stream.of(
        PullTransferPanel.class,
        PushTransferPanel.class,
        ExportPanel.class
    ).map(BriefcasePreferences::forClass)
        .forEach(CleanPreferences::flush);
  }

  private static void flush(BriefcasePreferences appPreferences) {
    System.out.println("Cleaning saved keys on " + appPreferences.node);
    List<String> keys = appPreferences.keys();
    appPreferences.removeAll(keys);
    System.out.println("Removed keys:");
    keys.forEach(key -> System.out.println("\t-" + key));
  }

}
