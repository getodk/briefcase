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

package org.opendatakit.briefcase.model;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.apache.http.HttpHost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendatakit.briefcase.buildconfig.BuildConfig;

/**
 * This class is used to manage the applications preferences. It achieves this task, by using the standard
 * {@link java.util.prefs.Preferences Preferences API}.
 */
public class BriefcasePreferences {

  public static final String VERSION = BuildConfig.VERSION;
  public static final String GOOGLE_TRACKING_ID = BuildConfig.GOOGLE_TRACKING_ID;
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String AGGREGATE_1_0_URL = "url_1_0";

  private static final String BRIEFCASE_DIR_PROPERTY = "briefcaseDir";
  private static final String BRIEFCASE_PROXY_HOST_PROPERTY = "briefcaseProxyHost";
  private static final String BRIEFCASE_PROXY_PORT_PROPERTY = "briefcaseProxyPort";
  private static final String BRIEFCASE_PARALLEL_PULLS_PROPERTY = "briefcaseParallelPulls";
  private static final String BRIEFCASE_TRACKING_CONSENT_PROPERTY = "briefcaseTrackingConsent";
  private static final String BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY = "briefcaseStorePasswordsConsent";
  private static final String BRIEFCASE_UNIQUE_USER_ID_PROPERTY = "uniqueUserID";

  static {
    // load the security provider
    Security.addProvider(new BouncyCastleProvider());
  }

  private final Preferences preferences;
  public Class<?> node;

  public BriefcasePreferences(Preferences preferences) {
    this.preferences = preferences;
  }

  private BriefcasePreferences(Class<?> node, PreferenceScope scope) {
    this.node = node;
    this.preferences = scope.preferenceFactory(node);
  }

  /**
   * Factory that creates a class scoped <tt>BriefcasePreferences</tt> object.
   *
   * @param node the managing class
   */
  public static BriefcasePreferences forClass(Class<?> node) {
    return new BriefcasePreferences(node, PreferenceScope.CLASS_NAME);
  }

  /**
   * Returns the value associated with the specified key in this preference
   * node. Returns the specified default if there is no value associated with
   * the key, or the backing store is inaccessible.
   *
   * @param key          key whose associated value is to be returned.
   * @param defaultValue the value to be returned in the event that this preference node
   *                     has no value associated with key.
   * @return the value associated with key, or defaultValue if no value is associated
   * with key, or the backing store is inaccessible.
   */
  public String get(String key, String defaultValue) {
    return preferences.get(key, defaultValue);
  }

  /**
   * Returns an Optional instance with the value associated with the specified key
   * in this preference node or an Optional.empty() if no value is associated with key,
   * or the backing store is inaccessible.
   *
   * @param key key whose associated value is to be returned.
   * @return an Optional instance with the value associated with key, or Optional.empty()
   * if no value is associated with key, or the backing store is inaccessible.
   */
  public Optional<String> nullSafeGet(String key) {
    return Optional.ofNullable(get(key, null));
  }

  /**
   * Associates the specified value with the specified key in this preference
   * node.
   * <p>
   * If the value is null, then the key is removed
   *
   * @param key   key with which the specified value is to be associated.
   * @param value value to be associated with the specified key or null.
   */
  public void put(String key, String value) {
    if (value != null)
      preferences.put(key, value);
    else
      remove(key);
  }

  /**
   * Associates the specified key/value map in this preference node.
   *
   * @param keyValues map of keys and values to ve associated.
   */
  public void putAll(Map<String, String> keyValues) {
    keyValues.forEach(this::put);
  }

  /**
   * Removes the value associated with the specified key in this preference
   * node, if any.
   *
   * @param key key whose mapping is to be removed from the preference node.
   */
  public void remove(String key) {
    preferences.remove(key);
  }

  /**
   * Removes all the values associated with the specified key list in this preference
   * node, if any.
   *
   * @param keys keys whose mappings are to be removed from the preference node.
   */
  public void removeAll(List<String> keys) {
    keys.forEach(this::remove);
  }

  public static void setBriefcaseDirectoryProperty(String value) {
    if (value == null) {
      Preference.APPLICATION_SCOPED.remove(BRIEFCASE_DIR_PROPERTY);
    } else {
      Preference.APPLICATION_SCOPED.put(BRIEFCASE_DIR_PROPERTY, value);
    }
  }

  public String getBriefcaseDirectoryOrNull() {
    return get(BRIEFCASE_DIR_PROPERTY, null);
  }

  public String getBriefcaseDirectoryOrUserHome() {
    try {
      return get(BRIEFCASE_DIR_PROPERTY, Files.createTempDirectory("briefcase").toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setBriefcaseProxyProperty(HttpHost value) {
    if (value == null) {
      Preference.APPLICATION_SCOPED.remove(BRIEFCASE_PROXY_HOST_PROPERTY);
      Preference.APPLICATION_SCOPED.remove(BRIEFCASE_PROXY_PORT_PROPERTY);
    } else {
      Preference.APPLICATION_SCOPED.put(BRIEFCASE_PROXY_HOST_PROPERTY, value.getHostName());
      Preference.APPLICATION_SCOPED.put(BRIEFCASE_PROXY_PORT_PROPERTY, Integer.toString(value.getPort()));
    }
  }

  public static void setBriefcaseParallelPullsProperty(Boolean value) {
    if (value == null) {
      Preference.APPLICATION_SCOPED.remove(BRIEFCASE_PARALLEL_PULLS_PROPERTY);
    } else {
      Preference.APPLICATION_SCOPED.put(BRIEFCASE_PARALLEL_PULLS_PROPERTY, value.toString());
    }
  }

  public static Boolean getBriefcaseParallelPullsProperty() {
    return Boolean.valueOf(
        Preference.APPLICATION_SCOPED.get(BRIEFCASE_PARALLEL_PULLS_PROPERTY, Boolean.FALSE.toString())
    );
  }

  /**
   * Enum that implements the strategies, to create differently scoped preferences.
   */
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

  /**
   * Through this static nested class, we implement the Initialization-on-demand
   * holder idiom. It enables a safe, highly concurrent lazy initialization for
   * singletons. For more information on this idiom, please refer to <a href=
   * "https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">
   * Initialization-on-demand holder idiom</a>.
   */
  private static class Preference {
    private static final BriefcasePreferences APPLICATION_SCOPED =
        new BriefcasePreferences(BriefcasePreferences.class, PreferenceScope.APPLICATION);
  }

  public static BriefcasePreferences appScoped() {
    return Preference.APPLICATION_SCOPED;
  }

  public static HttpHost getBriefCaseProxyConnection() {
    String host = Preference.APPLICATION_SCOPED.get(BRIEFCASE_PROXY_HOST_PROPERTY, null);
    if (host != null) {
      Integer port = Integer.parseInt(Preference.APPLICATION_SCOPED.get(
          BRIEFCASE_PROXY_PORT_PROPERTY, "0"));
      return new HttpHost(host, port);
    }
    return null;
  }

  /**
   * Persist the user's decision to allow/disallow their behaviour being tracked.
   *
   * @param value (required) the boolean value representing the user's decision.
   */
  public static void setBriefcaseTrackingConsentProperty(boolean value) {
    setBooleanProperty(BRIEFCASE_TRACKING_CONSENT_PROPERTY, value);
  }

  /**
   * Get the user's persisted decision regarding their consent to being tracked.
   *
   * @return the boolean representation of the user's consent to being tracked.
   */
  public static boolean getBriefcaseTrackingConsentProperty() {
    return getBooleanProperty(BRIEFCASE_TRACKING_CONSENT_PROPERTY);
  }

  public static boolean getStorePasswordsConsentProperty() {
    return getBooleanProperty(BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY);
  }

  public static void setStorePasswordsConsentProperty(boolean value) {
    setBooleanProperty(BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY, value);
  }

  private static void setBooleanProperty(String key, boolean value) {
    Preference.APPLICATION_SCOPED.put(key, Boolean.valueOf(value).toString());
  }

  private static boolean getBooleanProperty(String key) {
    return Boolean.valueOf(Preference.APPLICATION_SCOPED.get(key, Boolean.FALSE.toString()));
  }

  /**
   * Get the persisted UUID for the current user.
   * <P>Note that the method will generate and persist a UUID if the user doesn't have one.</P>
   *
   * @return the String representation of the user's UUID.
   */
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
