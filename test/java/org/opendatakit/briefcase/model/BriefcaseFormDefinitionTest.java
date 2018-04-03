package org.opendatakit.briefcase.model;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.SwingTestRig.classPath;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendatakit.briefcase.util.BadFormDefinition;

@RunWith(value = Parameterized.class)
public class BriefcaseFormDefinitionTest {
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public String expectedVersion;
  private Path formsDir;
  private Path form;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Null version", null},
        {"Numeric version", "1"},
        {"Alphanumeric version", "vHG9gooNbDqA2DxdU9cP5Q"},
    });
  }

  @Before
  public void setUp() throws IOException {
    formsDir = createTempDirectory("briefcase_test");
    form = formsDir.resolve("form.xml");
    String tpl = new String(readAllBytes(classPath("org/opendatakit/aggregate/parser/test-version.tpl.xml")), Charset.defaultCharset());
    write(form, String.format(
        tpl,
        expectedVersion != null ? "version=\"" + expectedVersion + "\"" : ""
    ).getBytes());
  }

  @After
  public void tearDown() throws IOException {
    delete(form);
    delete(formsDir);
  }

  @Test
  public void accepts_null_version() throws BadFormDefinition {
    BriefcaseFormDefinition bfd = new BriefcaseFormDefinition(form.getParent().toFile(), form.toFile());

    assertThat(bfd.getVersionString(), is(expectedVersion));
  }


}