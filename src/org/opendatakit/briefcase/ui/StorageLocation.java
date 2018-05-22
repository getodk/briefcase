package org.opendatakit.briefcase.ui;

import static org.opendatakit.briefcase.util.FileSystemUtils.FORMS_DIR;

import java.io.File;

/**
 * Managing the Briefcase storage location
 */
public class StorageLocation {
  private static final String BRIEFCASE_DIR = "ODK Briefcase Storage";

  /**
   * Returns whether pathname is a folder under the Briefcase storage folder
   *
   * @param pathname the File to check
   */
  public static boolean isUnderBriefcaseFolder(File pathname) {
    File current = pathname;
    File parent = pathname == null ? null : pathname.getParentFile();
    while (parent != null) {
      if (isStorageLocationParent(parent) && current.getName().equals(StorageLocation.BRIEFCASE_DIR)) {
        return true;
      }
      current = parent;
      parent = parent.getParentFile();
    }
    return false;
  }

  private static boolean isStorageLocationParent(File pathname) {
    if (!pathname.exists()) {
      return false;
    }
    File folder = new File(pathname, StorageLocation.BRIEFCASE_DIR);
    if (!folder.exists() || !folder.isDirectory()) {
      return false;
    }
    File forms = new File(folder, FORMS_DIR);
    return forms.exists() && forms.isDirectory();
  }
}
