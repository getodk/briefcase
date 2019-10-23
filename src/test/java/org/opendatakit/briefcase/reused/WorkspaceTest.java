package org.opendatakit.briefcase.reused;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.preferences.InMemoryPreferences;

public class WorkspaceTest {

  private Workspace workspace;

  @Before
  public void setUp() {
    workspace = new Workspace(InMemoryPreferences.empty());
    workspace.setWorkspaceLocation(Paths.get("/some/workspace/location"));
  }

  @Test
  public void the_form_directory_is_based_on_the_form_id_and_version() {
    assertThat(
        workspace.buildFormFile(buildFormMetadata("form id", Optional.of("form version"), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("form id(Some form)").resolve("form id(Some form)[form version].xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata("form id", Optional.empty(), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("form id(Some form)").resolve("form id(Some form).xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata("form id", Optional.of("form version"), Optional.empty())),
        is(workspace.getFormsDir().resolve("form id").resolve("form id[form version].xml"))
    );
  }

  @Test
  public void form_id_and_version_get_sanitized_for_filesystem_friendly_strings() {
    assertThat(
        workspace.buildFormFile(buildFormMetadata("fo.rm\tid", Optional.of("fo.rm\tversion"), Optional.of("So.me\tform"))),
        is(workspace.getFormsDir().resolve("fo_rm id(So_me form)").resolve("fo_rm id(So_me form)[fo_rm version].xml"))
    );
  }

  @Test
  public void an_hexadecimal_string_is_used_when_no_meaningful_names_can_be_produced_after_sanitizing_the_form_directory() {
    assertThat(
        workspace.buildFormFile(buildFormMetadata(",,,", Optional.empty(), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("2c2c2c(Some form)").resolve("2c2c2c(Some form).xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(":::", Optional.empty(), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("3a3a3a(Some form)").resolve("3a3a3a(Some form).xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(",,,", Optional.of("form version"), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("2c2c2c(Some form)").resolve("2c2c2c(Some form)[form version].xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(":::", Optional.of("form version"), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("3a3a3a(Some form)").resolve("3a3a3a(Some form)[form version].xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(",,,", Optional.of(",,,"), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("2c2c2c(Some form)").resolve("2c2c2c(Some form)[2c2c2c].xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(":::", Optional.of(":::"), Optional.of("Some form"))),
        is(workspace.getFormsDir().resolve("3a3a3a(Some form)").resolve("3a3a3a(Some form)[3a3a3a].xml"))
    );
  }

  @Test
  public void form_id_is_used_if_no_meaningful_name_can_be_produced_from_the_form_name() {
    assertThat(
        workspace.buildFormFile(buildFormMetadata("form id", Optional.of("form version"), Optional.of(",,,"))),
        is(workspace.getFormsDir().resolve("form id").resolve("form id[form version].xml"))
    );
    assertThat(
        workspace.buildFormFile(buildFormMetadata(",,,", Optional.of("form version"), Optional.of(",,,"))),
        is(workspace.getFormsDir().resolve("2c2c2c").resolve("2c2c2c[form version].xml"))
    );
  }

  private FormMetadata buildFormMetadata(String formId, Optional<String> formVersion, Optional<String> formName) {
    return new FormMetadata(
        FormKey.of(formId, formVersion),
        false,
        formName,
        Optional.empty(),
        Cursor.empty(),
        false,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
  }
}
