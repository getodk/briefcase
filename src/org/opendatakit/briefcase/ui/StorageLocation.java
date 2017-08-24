package org.opendatakit.briefcase.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;

import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.opendatakit.briefcase.util.FileSystemUtils.FORMS_DIR;

/** Managing the Briefcase storage location */
public class StorageLocation {
    private final Window appWindow;
    private final UiStateChangeListener uiStateChangeListener;
    public static final String BRIEFCASE_DIR = "ODK Briefcase Storage";
    static final String README_TXT = "readme.txt";
    private static final Log log = LogFactory.getLog(StorageLocation.class.getName());

    public StorageLocation(Window appWindow, UiStateChangeListener uiStateChangeListener) {
        this.appWindow = appWindow;
        this.uiStateChangeListener = uiStateChangeListener;
    }

    public static File getBriefcaseFolder() {
      return new File(new File(BriefcasePreferences
          .getBriefcaseDirectoryProperty()), BRIEFCASE_DIR);
    }

    public void establishBriefcaseStorageLocation(boolean showDialog) {
        // set the enabled/disabled status of the panels based upon validity of default briefcase directory.
        String briefcaseDir = BriefcasePreferences.getBriefcaseDirectoryIfSet();
        boolean reset = false;
        if ( briefcaseDir == null ) {
            reset = true;
        } else {
            File dir = new File(briefcaseDir);
            if ( !dir.exists() || !dir.isDirectory()) {
                reset = true;
            } else {
                File folder = StorageLocation.getBriefcaseFolder();
                if ( !folder.exists() || !folder.isDirectory()) {
                    reset = true;
                }

            }
        }

        if ( showDialog || reset ) {
            // ask for new briefcase location...
            BriefcaseStorageLocationDialog fs =
                    new BriefcaseStorageLocationDialog(appWindow);

            // If reset is not needed, dialog has been triggered from Settings page
            if (!reset) {
                fs.updateForSettingsPage();
            }

            fs.setVisible(true);
            if ( fs.isCancelled() ) {
                // if we need to reset the briefcase location,
                // and have cancelled, then disable the UI.
                // otherwise the value we have is fine.
                uiStateChangeListener.setFullUIEnabled(!reset);
            } else {
                String briefcasePath = BriefcasePreferences.getBriefcaseDirectoryIfSet();
                if ( briefcasePath == null ) {
                    // we had a bad path -- disable all but Choose...
                    uiStateChangeListener.setFullUIEnabled(false);
                } else {
                    uiStateChangeListener.setFullUIEnabled(true);
                }
            }
        } else {
            File f = new File( BriefcasePreferences.getBriefcaseDirectoryProperty());
            if (BriefcaseFolderChooser.testAndMessageBadBriefcaseStorageLocationParentFolder(f, appWindow)) {
                try {
                    assertBriefcaseStorageLocationParentFolder(f);
                    uiStateChangeListener.setFullUIEnabled(true);
                } catch (FileSystemException e1) {
                    String msg = "Unable to create " + StorageLocation.BRIEFCASE_DIR;
                    log.error(msg, e1);
                    ODKOptionPane.showErrorDialog(appWindow, msg, "Failed to Create " + StorageLocation.BRIEFCASE_DIR);
                    // we had a bad path -- disable all but Choose...
                    uiStateChangeListener.setFullUIEnabled(false);
                }
            } else {
                // we had a bad path -- disable all but Choose...
                uiStateChangeListener.setFullUIEnabled(false);
            }
        }
    }

    public static final boolean isBriefcaseStorageLocationParentFolder(File pathname) {
      if ( !pathname.exists() ) {
        return false;
      }
      File folder = new File(pathname, StorageLocation.BRIEFCASE_DIR);
      if ( !folder.exists() ) {
        return false;
      }
      if ( !folder.isDirectory() ) {
        return false;
      }
      File forms = new File(folder, FORMS_DIR);
      if ( !forms.exists() ) {
        return false;
      }
      if ( !forms.isDirectory() ) {
        return false;
      }
      return true;
    }

    public static final void assertBriefcaseStorageLocationParentFolder(File pathname) throws FileSystemException {
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

    public static boolean isUnderBriefcaseFolder(File pathname) {
      File parent = (pathname == null ? null : pathname.getParentFile());
      File current = pathname;
      while (parent != null) {
        if (isBriefcaseStorageLocationParentFolder(parent) &&
            current.getName().equals(StorageLocation.BRIEFCASE_DIR)) {
          return true;
        }
        current = parent;
        parent = parent.getParentFile();
      }
      return false;
    }

}
