package org.opendatakit.briefcase.pull.aggregate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

public class AggregateAttachmentTest {
  @Test
  public void computes_the_string_MD5_hash_of_a_file() throws URISyntaxException, IOException {
    Path file = Paths.get(AggregateAttachmentTest.class.getResource("/org/opendatakit/briefcase/pull/aggregate/lorem-ipsum-40k.txt").toURI());
    String expectedHash = "E79C0D4AD451003BA8CFDC1183AC89E9";
    assertThat(AggregateAttachment.md5(file), Matchers.is(expectedHash));
  }

  @Test
  public void gets_download_url() throws URISyntaxException, MalformedURLException {
    AggregateAttachment testAttachment = AggregateAttachment.of("file_1", "123456", "file://org/opendatakit/briefcase/pull/aggregate/lorem-ipsum-40k.txt");
    URL expectedUrl = new URL("file://org/opendatakit/briefcase/pull/aggregate/lorem-ipsum-40k.txt");
    assertEquals(expectedUrl, testAttachment.getDownloadUrl());
  }

  @Test
  public void checks_equality_of_attachment() throws URISyntaxException, MalformedURLException {
    AggregateAttachment testAttachment = AggregateAttachment.of("file_1", "123456", "file://org/opendatakit/briefcase/pull/aggregate/lorem-ipsum-40k.txt");
    AggregateAttachment testAttachment2 = AggregateAttachment.of("file_1", "123456", "file://org/opendatakit/briefcase/pull/aggregate/lorem-ipsum-40k.txt");
    AggregateAttachment testAttachment3 = AggregateAttachment.of("file_3", "1234567", "file://org/opendatakit/briefcase/lorem-ipsum-40k.txt");
    assertFalse(testAttachment.equals(null));
    assertTrue(testAttachment.equals(testAttachment));
    assertTrue(testAttachment.equals(testAttachment2));
    assertFalse(testAttachment.equals(testAttachment3));
  }
}
