package org.opendatakit.briefcase.model.form;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.OffsetDateTime;
import org.junit.Test;

public class SubmissionExportMetadataTest {
  @Test
  public void knows_if_it_is_from_before_some_datetime_value() {
    SubmissionExportMetadata metadata = new SubmissionExportMetadata(
        "some instance ID",
        OffsetDateTime.parse("2019-07-01T00:00:00.000Z"),
        OffsetDateTime.parse("2019-07-01T00:00:00.000Z")
    );

    assertThat(metadata.isBefore(OffsetDateTime.parse("2019-06-01T00:00:00.000Z")), is(false));
    assertThat(metadata.isBefore(OffsetDateTime.parse("2019-07-01T00:00:00.000Z")), is(false));
    assertThat(metadata.isBefore(OffsetDateTime.parse("2019-08-01T00:00:00.000Z")), is(true));
  }
}
