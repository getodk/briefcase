/*
 * Copyright (C) 2012 University of Washington.
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

package org.opendatakit.briefcase.util;

/**
 * Originally written by Dylan.  Determines the mounts that have SD Cards attached.
 *
 * @author the.dylan.price@gmail.com
 * @author mitchellsundt@gmail.com
 */
public class FindDirectoryStructure {
  private static final String PROPERTY_OS = "os.name";
  private static final String OS_MAC = "Mac";

  public static boolean isMac() {
    String os = System.getProperty(PROPERTY_OS);
    return os.contains(OS_MAC);
  }

  public static boolean isUnix() {
    String os = System.getProperty(PROPERTY_OS).toLowerCase();
    return (os.contains("nix") || os.contains("nux") || os.contains("aix"));
  }

  public static boolean isWindows() {
    String os = System.getProperty(PROPERTY_OS).toLowerCase();
    return os.contains("windows");
  }

  public static String getOsName() {
    return isMac() ? "mac" : isWindows() ? "windows" : "linux";
  }
}
