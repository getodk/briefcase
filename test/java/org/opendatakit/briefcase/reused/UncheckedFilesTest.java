package org.opendatakit.briefcase.reused;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.opendatakit.briefcase.reused.UncheckedFiles.*;
import static org.mockito.Mockito.*;

public class UncheckedFilesTest {
    private static Path tempDir;
    private Path temp;


    @Before
    public void setUp() {
        tempDir = createTempDirectory("briefcase");
        temp = Paths.get(tempDir.toString() + "/test.txt");
        createFile(temp);
    }

    @After
    public void tearDown() {
        deleteRecursive(tempDir);
    }

    @Test(expected = UncheckedIOException.class)
    public void newInputStream_should_throw_exception() {
        UncheckedFiles.delete(temp);
        UncheckedFiles.newInputStream(Paths.get(tempDir.toString() + "/test.txt"));
    }

    @Test
    public void closeInputStream_should_handle_null() {
        UncheckedFiles.closeInputStream(null);
    }

    @Test(expected = IOException.class)
    public void closeInputStream_should_close() throws IOException {
        InputStream inputStream = UncheckedFiles.newInputStream(Paths.get(tempDir.toString() + "/test.txt"));
        // close inputStream
        UncheckedFiles.closeInputStream(inputStream);

        // after close, .available() should throw exception
        inputStream.available();
    }

    @Test(expected = UncheckedIOException.class)
    public void closeInputStream_should_throw_exception() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        doThrow(new IOException()).when(inputStream).close();

        UncheckedFiles.closeInputStream(inputStream);
    }
}
