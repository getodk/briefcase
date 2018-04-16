package org.opendatakit.briefcase.util;


import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.util.PrivateKeyUtils.isValidPrivateKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendatakit.briefcase.ui.SwingTestRig;

public class PrivateKeyUtilsTest {
  @BeforeClass
  public static void setUpBouncyCastle() {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @Test
  public void a_non_existing_path_should_not_be_a_valid_private_key() {
    assertThat(
        isValidPrivateKey(Paths.get("/non/existing/path")),
        is(false)
    );
  }

  @Test
  public void a_non_readable_path_should_not_be_a_valid_private_key() throws IOException {
    assertThat(
        isValidPrivateKey(createTempFile("briefcase", "test", asFileAttribute(fromString("---------")))),
        is(false)
    );
  }

  @Test
  public void a_directory_path_should_not_be_a_valid_private_key() throws IOException {
    assertThat(
        isValidPrivateKey(createTempDirectory("briefcase")),
        is(false)
    );
  }

  @Test
  public void a_file_with_some_plain_text_should_not_be_a_valid_private_key() throws IOException {
    // This is the last negative test case for paths that apparently should return always false
    Path file = createTempFile("briefcase", "test");
    Files.write(file, "This is not a PEM file".getBytes());
    assertThat(
        isValidPrivateKey(file),
        is(false)
    );
  }

  @Test
  public void a_file_with_PEM_content_should_return_true() throws IOException {
    Path file = createTempFile("briefcase", "test");
    Files.copy(SwingTestRig.classPath("/private-key.pem"), file, REPLACE_EXISTING);
    assertThat(
        isValidPrivateKey(file),
        is(true)
    );
  }
}