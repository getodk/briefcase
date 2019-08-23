package org.opendatakit.briefcase.model;

import static java.nio.file.Paths.get;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import org.junit.Test;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;

public class FormStatusTest {
  @Test
  public void knows_how_to_build_paths_to_assets_inside_the_storage_directory() {
    FormStatus form = new FormStatus(FormMetadata.empty(FormKey.of("Form #1", "form-1")).withFormFile(Paths.get("/storage/directory/forms/Form _1/Form _1.xml")));
    assertThat(
        form.getFormDir(),
        is(get("/storage/directory/forms/Form _1"))
    );
    assertThat(
        form.getFormFile(),
        is(get("/storage/directory/forms/Form _1/Form _1.xml"))
    );
    assertThat(
        form.getFormMediaDir(),
        is(get("/storage/directory/forms/Form _1/Form _1-media"))
    );
    assertThat(
        form.getFormMediaFile("form attachment #1.jpg"),
        is(get("/storage/directory/forms/Form _1/Form _1-media/form attachment #1.jpg"))
    );
    assertThat(
        form.getSubmissionsDir(),
        is(get("/storage/directory/forms/Form _1/instances"))
    );
    assertThat(
        form.getSubmissionDir("uuid:520e7b86-1572-45b1-a89e-7da26ad1624e"),
        is(get("/storage/directory/forms/Form _1/instances/uuid520e7b86-1572-45b1-a89e-7da26ad1624e"))
    );
    assertThat(
        form.getSubmissionMediaFile("uuid:520e7b86-1572-45b1-a89e-7da26ad1624e", "submission attachment #1.jpg"),
        is(get("/storage/directory/forms/Form _1/instances/uuid520e7b86-1572-45b1-a89e-7da26ad1624e/submission attachment #1.jpg"))
    );
  }

}
