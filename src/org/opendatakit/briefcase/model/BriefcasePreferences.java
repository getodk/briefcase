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

import java.security.Security;
import java.util.prefs.Preferences;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This class is used to manage the applications preferences. It achieves this task, by using the standard
 * {@link java.util.prefs.Preferences Preferences API}.
 */
public class BriefcasePreferences {
  
  public static final String VERSION = "v1.4.9 Production";
  public static final String USERNAME = "username";
  public static final String TOKEN = "token";
  public static final String AGGREGATE_1_0_URL = "url_1_0";
  public static final String AGGREGATE_0_9_X_URL = "url_0_9_X";
  
  private static final String BRIEFCASE_DIR_PROPERTY = "briefcaseDir";
  
  static {
    // load the security provider
    Security.addProvider(new BouncyCastleProvider());
  }

  private final Preferences preferences;
  
  private BriefcasePreferences(Class<?> node, PreferenceScope scope) {
    this.preferences = scope.preferenceFactory(node);
  }
  
  /**
   * Factory that returns the application scoped <tt>BriefcasePreferences</tt> object.
   * The method always return the same instance.
   */
  public static BriefcasePreferences applicationScoped() {
    return Preference.APPLICATION_SCOPED;
  }
  
  /**
   * Factory that creates a class scoped <tt>BriefcasePreferences</tt> object. 
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
   * @param key
   *          key whose associated value is to be returned.
   * @param defaultValue
   *          the value to be returned in the event that this preference node
   *          has no value associated with key.
   * @return the value associated with key, or defaultValue if no value is associated
   *         with key, or the backing store is inaccessible.
   */
  public String get(String key, String defaultValue) {
    return preferences.get(key, defaultValue);
  }
  
  /**
   * Associates the specified value with the specified key in this preference
   * node.
   * 
   * @param key
   *          key with which the specified value is to be associated.
   * @param value
   *          value to be associated with the specified key.
   */
  public void put(String key, String value) {
    preferences.put(key, value);
  }
  
  /**
   * Removes the value associated with the specified key in this preference
   * node, if any.
   * 
   * @param key
   *          key whose mapping is to be removed from the preference node.
   */
  public void remove(String key) {
    preferences.remove(key);
  }

  public static void setBriefcaseDirectoryProperty(String value) {
    if (value == null) {
      Preference.APPLICATION_SCOPED.remove(BRIEFCASE_DIR_PROPERTY);
    } else {
      Preference.APPLICATION_SCOPED.put(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY, value);
    }
  }

  public static String getBriefcaseDirectoryIfSet() {
    return Preference.APPLICATION_SCOPED.get(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY, null);
  }

  public static String getBriefcaseDirectoryProperty() {
    return Preference.APPLICATION_SCOPED.get(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY,
        System.getProperty("user.home"));
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
   * Static member class, that holds the instance to the application scoped
   * <tt>BriefcasePreferences</tt> object. Initializing the instance in this class, enables us to lazy-load the instance when
   * {@link org.opendatakit.briefcase.model.BriefcasePreferences#applicationScoped
   * BriefcasePreferences.applicationScoped()} is executed. The initialization is therefore not
   * bound to the loading of the <tt>BriefcasePreferences</tt> class.
   */
  private static class Preference {
    private static final BriefcasePreferences APPLICATION_SCOPED =
        new BriefcasePreferences(BriefcasePreferences.class, PreferenceScope.APPLICATION);
  }
}
