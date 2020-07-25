package org.opendatakit.briefcase.reused;

import static org.opendatakit.briefcase.reused.UncheckedFiles.createFile;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class UncheckedFilesInputStreamTest {
  private Path tempDir;
  private Path temp;


  @Before
  public void setUp() {
    tempDir = createTempDirectory("briefcase");
    temp = tempDir.resolve("test.txt");
    createFile(temp);
  }

  @After
  public void tearDown() {
    deleteRecursive(tempDir);
  }

  @Test
  public void closeInputStream_should_handle_null() {
    UncheckedFiles.closeInputStream(null);
  }

  @Test(expected = IOException.class)
  public void closeInputStream_should_close() throws IOException {
    InputStream inputStream = UncheckedFiles.newInputStream(temp);

    UncheckedFiles.closeInputStream(inputStream);

    // after close, .available() should throw exception
    inputStream.available();
  }

  @Test(expected = UncheckedIOException.class)
  public void closeInputStream_should_throw_exception() throws IOException {
    InputStream is = new InputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }
      @Override
      public void close() throws IOException {
        throw new IOException("chuchu");
      }
    };
    UncheckedFiles.closeInputStream(is);
  }
}
