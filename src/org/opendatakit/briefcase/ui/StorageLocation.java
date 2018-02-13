package org.opendatakit.briefcase.ui;

import static org.opendatakit.briefcase.util.FileSystemUtils.FORMS_DIR;

import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Managing the Briefcase storage location */
public class StorageLocation {
    static final String BRIEFCASE_DIR = "ODK Briefcase Storage";
    static final String README_TXT = "readme.txt";
    private final BriefcasePreferences briefcasePreferences;
    private static final Logger log = LoggerFactory.getLogger(StorageLocation.class.getName());

    public StorageLocation() {
        briefcasePreferences = BriefcasePreferences.appScoped();
    }

    public StorageLocation(BriefcasePreferences briefcasePreferences) {
        this.briefcasePreferences = briefcasePreferences;
    }

    /** Returns a File object representing the Briefcase storage location folder, using the directory provided
     * by the user if there is one, otherwise the user’s home directory. */
    public File getBriefcaseFolder() {
        File briefcaseFolderLocation = new File(briefcasePreferences.getBriefcaseDirectoryOrUserHome());
        return new File(briefcaseFolderLocation, BRIEFCASE_DIR);
    }

    /**
     * Sets the enabled/disabled status of the panels based upon the validity of the default briefcase directory.
     * @param errorParentWindow the parent window of error dialogs
     * @param uiStateChangeListener an object whose setFullUIEnabled method is to be called
     */
    void establishBriefcaseStorageLocation(Window errorParentWindow, UiStateChangeListener uiStateChangeListener) {
        String briefcaseDir = briefcasePreferences.getBriefcaseDirectoryOrNull();
        boolean enableUi = false;

        if (briefcaseDir != null) {
            File dir = new File(briefcaseDir);
            if (dir.exists() && dir.isDirectory()) {
                if (BriefcaseFolderChooser.testAndMessageBadBriefcaseStorageLocationParentFolder(dir, errorParentWindow)) {
                    try {
                        assertBriefcaseStorageLocationParentFolder(dir);
                        enableUi = true;
                    } catch (FileSystemException e1) {
                        String msg = "Unable to create " + StorageLocation.BRIEFCASE_DIR;
                        log.error(msg, e1);
                        ODKOptionPane.showErrorDialog(errorParentWindow, msg, "Failed to Create " + StorageLocation.BRIEFCASE_DIR);
                    }
                }
            }
        }
        uiStateChangeListener.setFullUIEnabled(enableUi);
    }

    void assertBriefcaseStorageLocationParentFolder(File pathname) throws FileSystemException {
      File folder = new File(pathname, StorageLocation.BRIEFCASE_DIR);
      if ( !folder.exists() ) {
        if ( !folder.mkdir() ) {
          throw new FileSystemException("Unable to create " + StorageLocation.BRIEFCASE_DIR);
        }
      }
      File forms = new File(folder, FORMS_DIR);
      if ( !forms.exists() ) {
        if ( !forms.mkdir() ) {
          throw new FileSystemException("Unable to create " + FORMS_DIR);
        }
      }

      File f = new File(folder, README_TXT);
      if ( !f.exists() ) {
        try {
          if ( !f.createNewFile() ) {
            throw new FileSystemException("Unable to create " + README_TXT);
          }
        } catch (IOException e) {
          String msg = "Unable to create " + README_TXT;
          log.error(msg, e);
          throw new FileSystemException(msg);
        }
      }
      try {
        OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream(f,false), "UTF-8");
        fout.write(MessageStrings.README_CONTENTS);
        fout.close();
      } catch (IOException e) {
        String msg = "Unable to write " + README_TXT;
        log.error(msg, e);
        throw new FileSystemException(msg);
      }
    }

    /**
     * Returns whether pathname is a folder under the Briefcase storage folder
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
