/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class BriefcasePreferences {
  private static final String BRIEFCASE_UNIQUE_USER_ID_PROPERTY = "uniqueUserID";
  public static final String BRIEFCASE_DIR = "ODK Briefcase Storage";

  private final Preferences preferences;
  public Class<?> node;

  public BriefcasePreferences(Preferences preferences) {
    this.preferences = preferences;
  }

  private BriefcasePreferences(Class<?> node, PreferenceScope scope) {
    this.node = node;
    this.preferences = scope.preferenceFactory(node);
  }

  public static BriefcasePreferences forClass(Class<?> node) {
    return new BriefcasePreferences(node, PreferenceScope.CLASS_NAME);
  }

  private String get(String key, String defaultValue) {
    return preferences.get(key, defaultValue);
  }

  private void put(String key, String value) {
    if (value != null)
      preferences.put(key, value);
    else
      remove(key);
  }

  public Optional<String> nullSafeGet(String key) {
    return Optional.ofNullable(get(key, null));
  }

  public void putAll(Map<String, String> keyValues) {
    keyValues.forEach(this::put);
  }

  public void remove(String key) {
    preferences.remove(key);
  }

  public void removeAll(List<String> keys) {
    keys.forEach(this::remove);
  }

  private enum PreferenceScope {
    APPLICATION {
      @Override
      public Preferences preferenceFactory(Class<?> node) {
        return Preferences.userNodeForPackage(node);
      }
    },
    CLASS_NAME {
      @Override
      public Preferences preferenceFactory(Class<?> node) {
        return Preferences.userRoot().node(node.getName());
      }
    };

    public abstract Preferences preferenceFactory(Class<?> node);
  }

  private static class Preference {
    private static final BriefcasePreferences APPLICATION_SCOPED =
        new BriefcasePreferences(BriefcasePreferences.class, PreferenceScope.APPLICATION);
  }

  public static BriefcasePreferences appScoped() {
    return Preference.APPLICATION_SCOPED;
  }

  public static String getUniqueUserID() {
    String defaultUuidValue = "UUID missing, defaulting to this message";
    String uniqueUserID = Preference.APPLICATION_SCOPED.get(BRIEFCASE_UNIQUE_USER_ID_PROPERTY, defaultUuidValue);
    if (uniqueUserID.equals(defaultUuidValue)) {
      Preference.APPLICATION_SCOPED.put(BRIEFCASE_UNIQUE_USER_ID_PROPERTY, UUID.randomUUID().toString());
    }
    return Preference.APPLICATION_SCOPED.get(BRIEFCASE_UNIQUE_USER_ID_PROPERTY, defaultUuidValue);
  }

  public List<String> keys() {
    try {
      return Arrays.asList(preferences.keys());
    } catch (BackingStoreException e) {
      throw new RuntimeException(e);
    }
  }
}
