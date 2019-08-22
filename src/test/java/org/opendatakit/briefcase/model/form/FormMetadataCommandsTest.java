package org.opendatakit.briefcase.model.form;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.cleanAllCursors;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateAsPulled;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateLastExportedSubmission;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommandsTest {

  private static final Path formDir = Paths.get("/some/path/");
  private static final Path formFilename = Paths.get("Some form.xml");
  private FormKey key;
  private FormMetadataPort formMetadataPort;

  @Before
  public void setUp() {
    key = FormKey.of("Some form", "some-form");
    FormMetadata formMetadata = new FormMetadata(key, formDir, formFilename, false, Cursor.empty(), Optional.empty());
    formMetadataPort = new InMemoryFormMetadataAdapter();
    formMetadataPort.persist(formMetadata);
  }

  @Test
  public void updates_a_form_as_having_been_pulled() {
    formMetadataPort.execute(updateAsPulled(key, formDir, formFilename));
    assertThat(formMetadataPort.fetch(key).get().hasBeenPulled(), is(true));
  }

  @Test
  public void updates_a_form_as_having_been_pulled_adding_the_last_cursor_used_to_pull_it() {
    Cursor cursor = Cursor.from("some cursor data");
    formMetadataPort.execute(updateAsPulled(key, cursor, formDir, formFilename));
    FormMetadata formMetadata = formMetadataPort.fetch(key).get();
    assertThat(formMetadata.hasBeenPulled(), is(true));
    assertThat(formMetadata.getCursor(), is(cursor));
  }

  @Test
  public void updates_a_forms_last_exported_submission_metadata() {
    OffsetDateTime expectedSubmissionDate = OffsetDateTime.parse("2019-01-01T00:00:00.000Z");
    String expectedInstanceId = "some uuid";
    OffsetDateTime expectedExportDate = OffsetDateTime.parse("2019-02-01T00:00:00.000Z");
    formMetadataPort.execute(updateLastExportedSubmission(key, expectedInstanceId, expectedSubmissionDate, expectedExportDate, formDir, formFilename));
    FormMetadata formMetadata = formMetadataPort.fetch(key).orElseThrow();
    assertThat(formMetadata.getLastExportedSubmissionDate(), isPresentAndIs(expectedSubmissionDate));
  }

  @Test
  public void updates_all_metadata_removing_cursors() {
    List<FormMetadata> existingFormMetadata = IntStream.range(0, 10).mapToObj(this::buildFormMetadata).collect(Collectors.toList());
    existingFormMetadata.forEach(formMetadataPort::persist);

    formMetadataPort.execute(cleanAllCursors());

    existingFormMetadata
        .stream()
        .map(FormMetadata::getKey)
        .forEach(key -> {
          FormMetadata actualFormMetadata = formMetadataPort.fetch(key).orElseThrow();
          assertThat(actualFormMetadata.getCursor().isEmpty(), is(true));
        });
  }

  private FormMetadata buildFormMetadata(int number) {
    Path formDir = Paths.get("/some/path/forms/Form " + number);
    Path formFilename = Paths.get("Some form " + number + ".xml");
    FormKey key = FormKey.of("Form " + number, "form-" + number);
    return new FormMetadata(key, formDir, formFilename, true, Cursor.from("some cursor data"), Optional.empty());
  }
}
