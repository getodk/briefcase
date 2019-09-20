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

import static org.opendatakit.briefcase.delivery.cli.Common.WORKSPACE_LOCATION;

import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.OperationBuilder;
import org.opendatakit.briefcase.reused.cli.Param;

public class ClearPreferences {

  private static Param<Void> CLEAR = Param.flag("c", "clear_prefs", "Clear saved preferences");

  public static Operation create(Container container) {
    return new OperationBuilder()
        .withFlag(CLEAR)
        .withOptionalParams(WORKSPACE_LOCATION)
        .withLauncher(args -> clear(container))
        .build();
  }

  private static void clear(Container container) {
    container.preferences.fetchAllKeys()
        .forEach(key -> System.out.println(String.format("(%s) %s", key.getCategory().getName(), key.getName())));
    container.preferences.flush();
  }
}
