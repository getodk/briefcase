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
package org.opendatakit.briefcase.cli;

import static java.util.Comparator.naturalOrder;

import java.util.List;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.ui.pull.PullPanel;
import org.opendatakit.briefcase.ui.push.PushPanel;

public class ClearPreferences {

  private static Param<Void> CLEAR = Param.flag("c", "clear_prefs", "Clear saved preferences");

  public static Operation create(FormMetadataPort formMetadataPort) {
    return Operation.of(CLEAR, args -> clear());
  }

  private static void clear() {
    flush(BriefcasePreferences.appScoped());
    Stream.of(
        PullPanel.class,
        PushPanel.class,
        ExportPanel.class
    ).map(BriefcasePreferences::forClass)
        .forEach(ClearPreferences::flush);
  }

  private static void flush(BriefcasePreferences appPreferences) {
    System.out.println("Clearing saved keys on " + appPreferences.node);
    List<String> keys = appPreferences.keys();
    appPreferences.removeAll(keys);
    keys.sort(naturalOrder());
    keys.forEach(key -> System.out.println("  " + key));
  }

}
