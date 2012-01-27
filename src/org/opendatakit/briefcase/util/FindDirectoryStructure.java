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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Originally written by Dylan.  Determines the mounts that have SD Cards attached.
 * 
 * @author the.dylan.price@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class FindDirectoryStructure {
  private static final Logger _logger = Logger.getLogger(FindDirectoryStructure.class.getName());
  private static final String PROPERTY_OS = "os.name";
  private static final String OS_WINDOWS = "Windows";
  private static final String OS_MAC = "Mac";

  /**
   * Searches mounted drives for /odk/instances and returns a list of
   * positive results. Works for Windows, Mac, and Linux operating
   * systems.
   * 
   * @return a List<File> containing matches of currently mounted file systems
   *         which contain the directoryStructureToSearchFor
   */
  public static List<File> searchMountedDrives() {
    String os = System.getProperty(PROPERTY_OS);
    _logger.info("OS reported as: " + os);
    if (os.contains(OS_WINDOWS)) {
      File[] drives = File.listRoots();
      return search(drives);
    } else if (os.contains(OS_MAC)) {
      File[] mounts = { new File("/Volumes"), new File("/media"), new File("/mnt") };
      return search(mounts);
    } else // Assume Unix
    {
      File[] mounts = { new File("/mnt"), new File("/media") };
      return search(mounts);
    }
  }

  /**
   * Checks each given potential directory for existence of odk/instances
   * under it and returns a list of positive matches.
   * 
   * @param mounts
   *          the potential mount points to check.
   * @return a List<File> containing mount points which contained the
   *         given directory structure underneath them
   */
  public static List<File> search(File[] mounts) {

    List<File> candidates = new ArrayList<File>();

    for ( File f : mounts ) {
      if ( f.exists() && f.isDirectory() ) {
        File fo = new File(f, "odk");
        if ( fo.exists() && f.isDirectory() ) {
          File foi = new File(fo, "instances");
          if ( foi.exists() && f.isDirectory() ) {
            candidates.add(f);
          }
        }
      }
    }
    return candidates;
  }
}
