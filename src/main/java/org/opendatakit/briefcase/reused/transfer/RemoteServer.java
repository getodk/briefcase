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
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.reused.Optionals;
import org.opendatakit.briefcase.reused.http.response.Response;

public interface RemoteServer {
  /**
   * Returns true if the given key is a prefs key managed by this class hierarchy.
   * <p>
   * Includes keys used to store the last configured source in the Pull & Push
   * panels, and the keys used to support the "pull before export" feature.
   */
  static boolean isPrefKey(String key) {
    return AggregateServer.isPrefKey(key) || CentralServer.isPrefKey(key);
  }

  /**
   * Stores in the given prefs object this RemoteServer's information used as a pull source or push target in the UI
   */
  void storeInPrefs(BriefcasePreferences prefs, boolean storePasswords);

  /**
   * Stores in the given prefs object this RemoteServer's information used to pull the given form.
   */
  void storeInPrefs(BriefcasePreferences prefs, boolean storePasswords, String formId);

  static void clearStoredPrefs(BriefcasePreferences prefs) {
    AggregateServer.clearStoredPrefs(prefs);
    CentralServer.clearStoredPrefs(prefs);
  }

  /**
   * Searches for keys used to store the last configured source in the Pull & Push
   * panels and returns a non-empty value when they're found.
   */
  @SuppressWarnings("unchecked")
  static <T extends RemoteServer> Optional<T> readFromPrefs(BriefcasePreferences prefs) {
    // Hacky way to get the correct subtype. Basically, try to de-serialize saved prefs
    // until one of the de-serializers successfully manages to get an instance
    return Optionals.race(
        AggregateServer.readFromPrefs(prefs).map(o -> (T) o),
        CentralServer.readFromPrefs(prefs).map(o -> (T) o)
    );
  }

  /**
   * Searches for keys used to store the last used pull source for
   * a form to support the "pull before export" feature and returns
   * a non-empty value when they're found.
   * <p>
   * This method requires both the app's prefs object and the pull
   * panel's prefs object to be able to be backwards-compatible with
   * older versions that store prefs using different keys
   */
  @SuppressWarnings("unchecked")
  static <T extends RemoteServer> Optional<T> readFromPrefs(BriefcasePreferences prefs, BriefcasePreferences pullPanelPrefs, FormKey formKey) {
    // Hacky way to get the correct subtype. Basically, try to de-serialize saved prefs
    // until one of the de-serializers successfully manages to get an instance
    return Optionals.race(
        AggregateServer.readFromPrefs(prefs, pullPanelPrefs, formKey).map(o -> (T) o),
        CentralServer.readFromPrefs(prefs, formKey).map(o -> (T) o)
    );
  }

  @FunctionalInterface
  interface Test<T extends RemoteServer> {
    Response test(T server);
  }
}
