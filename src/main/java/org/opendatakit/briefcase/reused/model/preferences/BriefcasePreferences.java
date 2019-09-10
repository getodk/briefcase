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
import org.apache.http.HttpHost;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.reused.api.OptionalProduct;

public class BriefcasePreferences {
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String AGGREGATE_1_0_URL = "url_1_0";

  private static final String BRIEFCASE_PROXY_HOST_PROPERTY = "briefcaseProxyHost";
  private static final String BRIEFCASE_PROXY_PORT_PROPERTY = "briefcaseProxyPort";
  private static final String BRIEFCASE_START_FROM_LAST_PROPERTY = "briefcaseResumeLastPull";
  private static final String BRIEFCASE_TRACKING_CONSENT_PROPERTY = "briefcaseTrackingConsent";
  private static final String BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY = "briefcaseStorePasswordsConsent";
  private static final String BRIEFCASE_UNIQUE_USER_ID_PROPERTY = "uniqueUserID";
  private static final String BRIEFCASE_MAX_HTTP_CONNECTIONS_PROPERTY = "maxHttpConnections";
  public static final String BRIEFCASE_DIR = "ODK Briefcase Storage";
  private static final String TRACKING_WARNING_SHOWED_PREF_KEY = "tracking warning showed";

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

  private void removeAll(String... keys) {
    removeAll(Arrays.asList(keys));
  }

  public void removeAll(List<String> keys) {
    keys.forEach(this::remove);
  }

  public Optional<HttpHost> getHttpProxy() {
    return OptionalProduct.all(
        nullSafeGet(BRIEFCASE_PROXY_HOST_PROPERTY),
        nullSafeGet(BRIEFCASE_PROXY_PORT_PROPERTY).map(Integer::parseInt)
    ).map(HttpHost::new);
  }

  public void setHttpProxy(HttpHost proxy) {
    put(BRIEFCASE_PROXY_HOST_PROPERTY, proxy.getHostName());
    put(BRIEFCASE_PROXY_PORT_PROPERTY, Integer.valueOf(proxy.getPort()).toString());
  }

  public void unsetHttpProxy() {
    removeAll(BRIEFCASE_PROXY_HOST_PROPERTY, BRIEFCASE_PROXY_PORT_PROPERTY);
  }

  public Optional<Boolean> getStartFromLast() {
    return nullSafeGet(BRIEFCASE_START_FROM_LAST_PROPERTY).map(Boolean::parseBoolean);
  }

  public Optional<Integer> getMaxHttpConnections() {
    return nullSafeGet(BRIEFCASE_MAX_HTTP_CONNECTIONS_PROPERTY).map(Integer::parseInt);
  }

  public void setMaxHttpConnections(int value) {
    put(BRIEFCASE_MAX_HTTP_CONNECTIONS_PROPERTY, String.valueOf(value));
  }

  public void setStartFromLast(Boolean enabled) {
    put(BRIEFCASE_START_FROM_LAST_PROPERTY, enabled.toString());
  }

  public void setRememberPasswords(Boolean enabled) {
    put(BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY, enabled.toString());
    EventBus.publish(enabled ? new SavePasswordsConsentGiven() : new SavePasswordsConsentRevoked());
  }

  public Optional<Boolean> getRememberPasswords() {
    return nullSafeGet(BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY).map(Boolean::parseBoolean);
  }

  public void setSendUsage(Boolean enabled) {
    put(BRIEFCASE_TRACKING_CONSENT_PROPERTY, enabled.toString());
  }

  public Optional<Boolean> getSendUsageData() {
    return nullSafeGet(BRIEFCASE_TRACKING_CONSENT_PROPERTY).map(Boolean::parseBoolean);
  }

  public void setTrackingWarningShowed() {
    put(TRACKING_WARNING_SHOWED_PREF_KEY, Boolean.TRUE.toString());
  }

  public boolean hasTrackingWarningBeenShowed() {
    return hasKey(TRACKING_WARNING_SHOWED_PREF_KEY);
  }

  public boolean resolveStartFromLast() {
    return getStartFromLast().orElse(false);
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

  public static boolean getBriefcaseTrackingConsentProperty() {
    return getBooleanProperty(BRIEFCASE_TRACKING_CONSENT_PROPERTY);
  }

  public static boolean getStorePasswordsConsentProperty() {
    return getBooleanProperty(BRIEFCASE_STORE_PASSWORDS_CONSENT_PROPERTY);
  }

  private static boolean getBooleanProperty(String key) {
    return Boolean.parseBoolean(Preference.APPLICATION_SCOPED.get(key, Boolean.FALSE.toString()));
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

  public boolean hasKey(String key) {
    return keys().contains(key);
  }

}
