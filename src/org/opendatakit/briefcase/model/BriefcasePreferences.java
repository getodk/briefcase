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

public class BriefcasePreferences {

  public static final String VERSION = "v1.2.3 Production";

  public static void setBriefcaseDirectoryProperty(String value) {
    if ( value == null ) {
      getApplicationPreferences().remove(BRIEFCASE_DIR_PROPERTY);
    } else {
      getApplicationPreferences().put(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY, value);
    }
  }

  public static String getBriefcaseDirectoryIfSet() {
    return getApplicationPreferences().get(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY,null);
  }

  public static String getBriefcaseDirectoryProperty() {
    return getApplicationPreferences().get(BriefcasePreferences.BRIEFCASE_DIR_PROPERTY,
        System.getProperty("user.home"));
  }

  private static final String BRIEFCASE_DIR_PROPERTY = "briefcaseDir";

  private static Preferences applicationPreferences = null;

  private static synchronized Preferences getApplicationPreferences() {
    if ( applicationPreferences == null ) {
      // as good a place as any to do one-time initialization...
      Security.addProvider(new BouncyCastleProvider());
      // and load the preferences...
      applicationPreferences = Preferences.userNodeForPackage(BriefcasePreferences.class);
    }
    return applicationPreferences;
  }

}
