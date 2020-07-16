package org.opendatakit.briefcase.model.form;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.cleanAllCursors;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateAsPulled;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateLastExportedSubmission;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class FormMetadataCommandsTest {

  private static final Path storageRoot = Paths.get("/some/path");
  private static final Path formDir = storageRoot.resolve("forms/Some form");
  private FormKey key;
  private FormMetadataPort formMetadataPort;

  @Before
  public void setUp() {
    key = FormKey.of("Some form", "some-form");
    FormMetadata formMetadata = new FormMetadata(key, storageRoot, formDir, false, Cursor.empty(), Optional.empty(), Collections.emptySet());
    formMetadataPort = new InMemoryFormMetadataAdapter();
    formMetadataPort.persist(formMetadata);
  }

  @Test
  public void updates_a_form_as_having_been_pulled() {
    formMetadataPort.execute(updateAsPulled(key, storageRoot, formDir));
    assertThat(formMetadataPort.fetch(key).get().hasBeenPulled(), is(true));
  }

  @Test
  public void updates_a_form_as_having_been_pulled_adding_the_last_cursor_used_to_pull_it() {
    Cursor cursor = Cursor.from("some cursor data");
    formMetadataPort.execute(updateAsPulled(key, cursor, storageRoot, formDir));
    FormMetadata formMetadata = formMetadataPort.fetch(key).get();
    assertThat(formMetadata.hasBeenPulled(), is(true));
    assertThat(formMetadata.getCursor(), is(cursor));
  }

  @Test
  public void updates_a_forms_last_exported_submission_metadata() {
    OffsetDateTime expectedSubmissionDate = OffsetDateTime.parse("2019-01-01T00:00:00.000Z");
    String expectedInstanceId = "some uuid";
    OffsetDateTime expectedExportDate = OffsetDateTime.parse("2019-02-01T00:00:00.000Z");
    formMetadataPort.execute(updateLastExportedSubmission(key, expectedInstanceId, expectedSubmissionDate, expectedExportDate, storageRoot, formDir));
    FormMetadata formMetadata = formMetadataPort.fetch(key).get();
    assertThat(formMetadata.getLastExportedSubmission(), isPresent());
    // Indirectly assert that the submission metadata is OK by serializing into
    // a JSON. This is to avoid adding getters that will be used only by tests
    ObjectNode jsonNode = formMetadata.getLastExportedSubmission().get().asJson(new ObjectMapper());
    assertThat(jsonNode.get("instanceId").asText(), is(expectedInstanceId));
    assertThat(jsonNode.get("submissionDate").asText(), is(expectedSubmissionDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
    assertThat(jsonNode.get("exportDateTime").asText(), is(expectedExportDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
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
          FormMetadata actualFormMetadata = formMetadataPort.fetch(key).orElseThrow(BriefcaseException::new);
          assertThat(actualFormMetadata.getCursor().isEmpty(), is(true));
        });
  }

  private FormMetadata buildFormMetadata(int number) {
    Paths.get("/some/path/forms/" + stripIllegalChars("Form #" + number));
    FormKey key = FormKey.of("Form #" + number, "form-" + number);
    return new FormMetadata(key, storageRoot, formDir, true, Cursor.from("some cursor data"), Optional.empty(), Collections.emptySet());
  }
}
