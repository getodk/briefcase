package org.opendatakit.briefcase.model.form;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.FormMetadataQueries.lastCursorOf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Test;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormMetadataQueriesTest {
  @Test
  public void queries_the_last_cursor_of_a_form() {
    FormKey key = FormKey.of("Some form", "some-form");
    Path storageDirectory = Paths.get("/some/path/to/the/form");
    FormMetadata formMetadata = new FormMetadata(key, storageDirectory, false, Cursor.empty(), Optional.empty());
    FormMetadataPort formMetadataPort = new InMemoryFormMetadataAdapter();
    formMetadataPort.persist(formMetadata);

    assertThat(formMetadataPort.query(lastCursorOf(key)), isPresentAndIs(Cursor.empty()));

    Cursor cursor = Cursor.from("some cursor");
    formMetadataPort.execute(FormMetadataCommands.updateAsPulled(key, cursor, storageDirectory));

    assertThat(formMetadataPort.query(lastCursorOf(key)), isPresentAndIs(cursor));
  }
}
