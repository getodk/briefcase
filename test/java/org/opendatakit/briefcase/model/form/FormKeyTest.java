package org.opendatakit.briefcase.model.form;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.ui.pull.FormInstallerTest.getPath;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.util.BadFormDefinition;

public class FormKeyTest {
  @Test
  public void regression_wrong_form_name_when_creating_keys_from_briefcase_form_defs() throws BadFormDefinition, URISyntaxException, IOException {
    String sanitizedName = stripIllegalChars("this-title-has-dashes");
    Path briefcaseFolder = Files.createTempDirectory("briefcase");
    Path formFile = briefcaseFolder.resolve("forms").resolve(sanitizedName).resolve(sanitizedName + ".xml");
    Path sourceFormFile = getPath("title-with-dashes-form.xml");
    Files.createDirectories(formFile.getParent());
    Files.copy(sourceFormFile, formFile);
    FormKey key1 = FormKey.of("this-title-has-dashes", "this-id-has-dashes");
    FormKey key2 = FormKey.from(new FormStatus(BriefcaseFormDefinition.resolveAgainstBriefcaseDefn(formFile.toFile(), false, briefcaseFolder.toFile())));
    assertThat(key1, is(key2));
  }
}
