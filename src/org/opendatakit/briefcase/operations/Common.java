package org.opendatakit.briefcase.operations;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.ui.StorageLocation;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Param;

class Common {
  private static final Log LOGGER = LogFactory.getLog(Common.class);

  static final Param<String> STORAGE_DIR = Param.arg("sd", "storage_directory", "Briefcase storage directory");
  static final Param<String> FORM_ID = Param.arg("id", "form_id", "Form ID");

  static void bootCache(String storageDir) {
    BriefcasePreferences.setBriefcaseDirectoryProperty(storageDir);
    File f = new StorageLocation().getBriefcaseFolder();

    if (!f.exists()) {
      boolean success = f.mkdirs();
      if (success) {
        LOGGER.info("Successfully created directory. Using: " + f.getAbsolutePath());
      } else {
        LOGGER.error("Unable to create directory: " + f.getAbsolutePath());
        System.exit(1);
      }
    } else if (f.exists() && !f.isDirectory()) {
      LOGGER.error("Not a directory.  " + f.getAbsolutePath());
      System.exit(1);
    } else if (f.exists() && f.isDirectory()) {
      LOGGER.info("Directory found, using " + f.getAbsolutePath());
    }

    if (BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull() != null) {
      FileSystemUtils.createFormCacheInBriefcaseFolder();
    }
  }
}
