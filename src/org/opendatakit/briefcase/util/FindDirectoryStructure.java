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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Originally written by Dylan.  Determines the mounts that have SD Cards attached.
 * 
 * @author the.dylan.price@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class FindDirectoryStructure {

  private static final Log log = LogFactory.getLog(FindDirectoryStructure.class);

  private static final String PROPERTY_OS = "os.name";
  private static final String OS_WINDOWS = "Windows";
  private static final String OS_MAC = "Mac";
  private static final String USER_NAME = "user.name";

  public static boolean isMac() {
    String os = System.getProperty(PROPERTY_OS);
    return os.contains(OS_MAC);
  }
  
  /**
   * Searches mounted drives for /odk/instances and returns a list of
   * positive results. Works for Windows, Mac, and Linux operating
   * systems.
   * 
   * @return a {@link List} of {@link File}  containing matches of currently mounted file systems
   *         which contain the directoryStructureToSearchFor
   */
  public static List<File> searchMountedDrives() {
    String os = System.getProperty(PROPERTY_OS);
    log.info("OS reported as: " + os);
    if (os.contains(OS_WINDOWS)) {
      File[] drives = File.listRoots();
      return search(drives, true);
    } else if (os.contains(OS_MAC)) {
      File[] mounts = { new File("/Volumes"), new File("/media"), new File("/mnt") };
      return search(mounts, false);
    } else // Assume Unix
    {
      String username = System.getProperty(USER_NAME);
      List<File> mountslist = new ArrayList<File>();
      mountslist.add( new File("/mnt"));
      mountslist.add( new File("/media"));

      File f = new File("/media", username);
      if (f.exists() && f.isDirectory()) {
        mountslist.add(f);
      } 

      f = new File("/run/media", username);
      if (f.exists() && f.isDirectory()) {
        mountslist.add(f);
      }
      return search(mountslist.toArray(new File[mountslist.size()]), false);
    }
  }

  private static boolean hasOdkInstancesDirectory(File f) {
    File fo = new File(f, "odk");
    if ( fo.exists() && f.isDirectory() ) {
      File foi = new File(fo, "instances");
      if ( foi.exists() && f.isDirectory() ) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Checks each given potential directory for existence of odk/instances
   * under it and returns a list of positive matches.
   * 
   * @param mounts
   *          the potential mount points to check.
   * @param isDirectChild
   *          true if the 'odk/instances' should be immediately underneath.
   *          false if it should be one level down ('.../odk/instances').
   * @return a {@link List} of {@link File} containing mount points which contained the
   *         given directory structure underneath them
   */
  private static List<File> search(File[] mounts, boolean isDirectChild) {

    List<File> candidates = new ArrayList<File>();

    for ( File f : mounts ) {
      if ( f.exists() && f.isDirectory() ) {
        if ( isDirectChild ) {
          if ( hasOdkInstancesDirectory(f) ) {
            candidates.add(f);
          }
        } else {
          File[] subdirs = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
              return f.isDirectory();
            }
          });
          
          for ( File s : subdirs ) {
            if ( hasOdkInstancesDirectory(s) ) {
              candidates.add(s);
            }
          }
        }
      }
    }
    return candidates;
  }
}
