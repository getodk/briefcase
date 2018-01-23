package org.opendatakit.briefcase.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class SwingTestRig {
  private static final Log log = LogFactory.getLog(SwingTestRig.class);

  public static void prepareBriefcaseStorageFolder() {
    try {
      BriefcasePreferences.setBriefcaseDirectoryProperty(Files.createTempDirectory("briefcase_test").toString());
      File briefcaseFolder = new StorageLocation().getBriefcaseFolder();
      if (briefcaseFolder.mkdirs())
        log.info("Created test Briefcase storage folder at " + briefcaseFolder);
      else
        throw new RuntimeException("Can't create test Briefcase storage folder");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void installFormsFrom(Path path) {
    Path briefcaseStorageFolder = Paths.get(new StorageLocation().getBriefcaseFolder().toURI());
    uncheckedWalk(path).forEach(sourcePath -> uncheckedCopy(
        sourcePath,
        briefcaseStorageFolder.resolve(path.getParent().relativize(sourcePath))
    ));
  }

  public static Path classPath(String location) {
    return Paths.get(uncheckedURLtoURI(SwingTestRig.class.getResource(location)));
  }

  public static void createCache() {
    FileSystemUtils.createFormCacheInBriefcaseFolder();
  }

  public static void uncheckedSleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI uncheckedURLtoURI(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static Stream<Path> uncheckedWalk(Path path) {
    try {
      return Files.walk(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void uncheckedCopy(Path source, Path destination, CopyOption... copyOptions) {
    try {
      Files.copy(source, destination, copyOptions);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
