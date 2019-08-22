package org.opendatakit.briefcase.model;

import static java.nio.file.Paths.get;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import org.junit.Test;

public class FormStatusTest {
  @Test
  public void knows_how_to_build_paths_to_assets_inside_the_storage_directory() {
    Path briefcaseDir = get("/storage/directory");
    TestFormDefinition formDef = new TestFormDefinition("Form #1", "form-1", "");
    FormStatus form = new FormStatus(formDef);
    assertThat(
        form.getFormDir(briefcaseDir),
        is(get("/storage/directory/forms/Form _1"))
    );
    assertThat(
        form.getFormFile(briefcaseDir),
        is(get("/storage/directory/forms/Form _1/Form _1.xml"))
    );
    assertThat(
        form.getFormMediaDir(briefcaseDir),
        is(get("/storage/directory/forms/Form _1/Form _1-media"))
    );
    assertThat(
        form.getFormMediaFile(briefcaseDir, "form attachment #1.jpg"),
        is(get("/storage/directory/forms/Form _1/Form _1-media/form attachment #1.jpg"))
    );
    assertThat(
        form.getSubmissionsDir(briefcaseDir),
        is(get("/storage/directory/forms/Form _1/instances"))
    );
    assertThat(
        form.getSubmissionDir(briefcaseDir, "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e"),
        is(get("/storage/directory/forms/Form _1/instances/uuid520e7b86-1572-45b1-a89e-7da26ad1624e"))
    );
    assertThat(
        form.getSubmissionMediaFile(briefcaseDir, "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e", "submission attachment #1.jpg"),
        is(get("/storage/directory/forms/Form _1/instances/uuid520e7b86-1572-45b1-a89e-7da26ad1624e/submission attachment #1.jpg"))
    );
  }

}
