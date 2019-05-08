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

package org.opendatakit.briefcase.push.central;

import static java.util.Collections.emptyMap;

import java.util.Map;

class CentralErrorMessage {
  final String message;
  final Map<String, String> details;

  CentralErrorMessage(String message, Map<String, String> details) {
    this.message = message;
    this.details = details;
  }

  public static CentralErrorMessage empty() {
    return new CentralErrorMessage("", emptyMap());
  }
}
