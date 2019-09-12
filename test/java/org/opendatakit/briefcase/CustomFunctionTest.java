package org.opendatakit.briefcase;

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.pull.aggregate.PullFromAggregateIntegrationTest;

public class CustomFunctionTest {
  private static Path getPath(String fileName) {
    return Optional.ofNullable(PullFromAggregateIntegrationTest.class.getClassLoader().getResource("org/opendatakit/briefcase/" + fileName))
        .map(url -> Paths.get(toURI(url)))
        .orElseThrow(RuntimeException::new);
  }

  @Test
  public void name() {
    FormDefinition formDefinition = FormDefinition.from(getPath("custom-function-form.xml"));
    // Assert anything about the form definition object
    // What's important is that we can get the form def object without errors
    assertThat(formDefinition.getFormId(), Matchers.is("custom-function-form"));
  }
}
