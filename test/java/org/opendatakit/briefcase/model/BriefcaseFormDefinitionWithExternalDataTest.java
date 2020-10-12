package org.opendatakit.briefcase.model;

import static java.nio.file.Files.delete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.opendatakit.briefcase.util.FileSystemUtils.getMediaDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.util.BadFormDefinition;

public class BriefcaseFormDefinitionWithExternalDataTest {
  private Path formDir;
  private Path formFile;
  private Path mediaDir;
  private Path mediaFile;

  @Before
  public void setUp() {
    try {
      formDir = Files.createTempDirectory("briefcase_test");
      formFile = formDir.resolve("form.xml");
      Path sourceFile = Paths.get(BriefcaseFormDefinitionWithExternalDataTest.class.getResource("form-with-external-secondary-instance.xml").toURI());
      Files.copy(sourceFile, formFile);

      mediaDir = getMediaDirectory(formDir.toFile()).toPath();
      Files.createDirectories(mediaDir);
      mediaFile = mediaDir.resolve("external-xml.xml");
      Path sourceMedia = Paths.get(BriefcaseFormDefinitionWithExternalDataTest.class.getResource("external-xml.xml").toURI());
      Files.copy(sourceMedia, mediaFile);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void buildsFormDef_whenDefinitionReferencesExternalSecondaryInstance() throws BadFormDefinition {
    BriefcaseFormDefinition briefcaseFormDefinition = new BriefcaseFormDefinition(formDir.toFile(), formFile.toFile());

    assertThat(briefcaseFormDefinition.getFormName(), is("Form with external secondary instance"));
  }

  @After
  public void tearDown() throws IOException {
    delete(formFile);
    delete(mediaFile);
    delete(mediaDir);
    delete(formDir);
  }
}
