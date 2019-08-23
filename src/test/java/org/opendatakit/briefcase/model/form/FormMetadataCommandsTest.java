package org.opendatakit.briefcase.model.form;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.cleanAllCursors;
import static org.opendatakit.briefcase.model.form.FormMetadataCommands.upsert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataCommandsTest {

  private static final Path formFile = Paths.get("/some/path/Some form.xml");
  private FormKey key;
  private FormMetadataPort formMetadataPort;

  @Before
  public void setUp() {
    key = FormKey.of("Some form", "some-form");
    FormMetadata formMetadata = new FormMetadata(key, Optional.of(formFile), Cursor.empty(), false, Optional.empty(), Optional.empty(), Optional.empty());
    formMetadataPort = new InMemoryFormMetadataAdapter();
    formMetadataPort.persist(formMetadata);
  }

  @Test
  public void upserts_metadata() {
    FormMetadata formMetadata = FormMetadata.empty(key).withFormFile(formFile);

    formMetadataPort.execute(upsert(formMetadata));
    assertThat(formMetadataPort.fetch(key).orElseThrow().getFormFile(), isPresentAndIs(formFile));

    Cursor cursor = Cursor.from("some cursor data");
    formMetadataPort.execute(upsert(formMetadata.withCursor(cursor)));
    assertThat(formMetadataPort.fetch(key).orElseThrow().getCursor(), is(cursor));
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
    Path formFile = Paths.get("/some/path/forms/Form " + number + "/Form " + number + ".xml");
    FormKey key = FormKey.of("Form " + number, "form-" + number);
    return new FormMetadata(key, Optional.of(formFile), Cursor.from("some cursor data"), false, Optional.empty(), Optional.empty(), Optional.empty());
  }
}
