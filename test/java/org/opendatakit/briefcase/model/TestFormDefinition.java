package org.opendatakit.briefcase.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendatakit.briefcase.util.BadFormDefinition;

public class TestFormDefinition extends BriefcaseFormDefinition {
  private static final File formDir;
  private static final File formFile;

  static {
    try {
      Path dir = Files.createTempDirectory("briefcase_test");
      Path sourceFile = Paths.get(TestFormDefinition.class.getResource("/basic.xml").toURI());
      Path file = dir.resolve("form.xml");
      Files.copy(sourceFile, file);
      formDir = dir.toFile();
      formFile = file.toFile();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private final int id;

  TestFormDefinition(int id) throws BadFormDefinition {
    super(formDir, formFile);
    this.id = id;
  }

  @Override
  public LocationType getFormLocation() {
    return LocationType.LOCAL;
  }

  @Override
  public String getFormName() {
    return "Form #" + id;
  }

  @Override
  public String getFormId() {
    return "" + id;
  }

  @Override
  public String getVersionString() {
    return "1";
  }
}
