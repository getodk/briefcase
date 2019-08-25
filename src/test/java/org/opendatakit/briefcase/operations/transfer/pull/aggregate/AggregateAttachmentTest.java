package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.Matchers;
import org.junit.Test;

public class AggregateAttachmentTest {
  @Test
  public void computes_the_string_MD5_hash_of_a_file() throws URISyntaxException, IOException {
    Path file = Paths.get(AggregateAttachmentTest.class.getResource("/org/opendatakit/briefcase/operations/transfer/pull/aggregate/lorem-ipsum-40k.txt").toURI());
    String expectedHash = "E79C0D4AD451003BA8CFDC1183AC89E9";
    assertThat(AggregateAttachment.md5(file), Matchers.is(expectedHash));
  }
}
