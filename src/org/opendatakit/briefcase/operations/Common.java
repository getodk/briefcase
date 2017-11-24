package org.opendatakit.briefcase.operations;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.ui.StorageLocation;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class Common {
  private static final Log LOGGER = LogFactory.getLog(Common.class);

  private static final Param<String> STORAGE_DIR = Param.arg("sd", "storage_directory", "Briefcase storage directory");
  static final Param<String> FORM_ID = Param.arg("f", "form_id", "Form ID");
  static final Param<String> DIR = Param.arg("dir", "directory", "Directory for export and import operations");

  public static Operation BOOT_CACHE = Operation.of(args -> bootCache(args.get(STORAGE_DIR)), Stream.of(STORAGE_DIR).collect(toSet()));

  public static void bootCache(String storageDir) {
    BriefcasePreferences.setBriefcaseDirectoryProperty(storageDir);
    File f = new StorageLocation().getBriefcaseFolder();

    if (!f.exists()) {
      boolean success = f.mkdirs();
      if (success) {
        LOGGER.info("Successfully created directory. Using: " + f.getAbsolutePath());
      } else {
        LOGGER.error("Unable to create directory: " + f.getAbsolutePath());
        System.exit(0);
      }
    } else if (f.exists() && !f.isDirectory()) {
      LOGGER.error("Not a directory.  " + f.getAbsolutePath());
      System.exit(0);
    } else if (f.exists() && f.isDirectory()) {
      LOGGER.info("Directory found, using " + f.getAbsolutePath());
    }

    if (BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull() != null) {
      FileSystemUtils.createFormCacheInBriefcaseFolder();
    }
  }
}
