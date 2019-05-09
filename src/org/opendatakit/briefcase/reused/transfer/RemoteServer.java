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

package org.opendatakit.briefcase.reused.transfer;

import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.Optionals;
import org.opendatakit.briefcase.reused.http.response.Response;

public interface RemoteServer {

  @SuppressWarnings("unchecked")
  static <T extends RemoteServer> Optional<T> readPreferences(BriefcasePreferences prefs) {
    // Hacky way to get the correct subtype. Basically, try to de-serialize saved prefs
    // until one of the de-serializers successfully manages to get an instance
    return Optionals.race(
        AggregateServer.readPreferences(prefs).map(o -> (T) o),
        CentralServer.readPreferences(prefs).map(o -> (T) o)
    );
  }

  @FunctionalInterface
  interface Test<T extends RemoteServer> {
    Response test(T server);
  }
}
