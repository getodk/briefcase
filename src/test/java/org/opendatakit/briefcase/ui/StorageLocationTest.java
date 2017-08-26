package org.opendatakit.briefcase.ui;

import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StorageLocationTest {

    @Test(expected = FileSystemException.class)
    public void canDetectBadStorageDirectory() throws FileSystemException {
        StorageLocation sl = new StorageLocation(null);
        final String nonExistingDirName = "oeunhasneoueaotuh";
        sl.assertBriefcaseStorageLocationParentFolder(new File(nonExistingDirName));
    }

    @Test(expected = FileSystemException.class)
    public void canDetectNotADirectory() throws FileSystemException, IOException {
        StorageLocation sl = new StorageLocation();
        sl.assertBriefcaseStorageLocationParentFolder(File.createTempFile("xxxx", null));
    }

    @Test
    public void canSetUpEmptyDirectory() throws FileSystemException, IOException {
        StorageLocation sl = new StorageLocation();
        final Path tempPath = Files.createTempDirectory("xxxx");
        sl.assertBriefcaseStorageLocationParentFolder(tempPath.toFile());
        assertTrue(Files.exists(tempPath.resolve(StorageLocation.BRIEFCASE_DIR).resolve(StorageLocation.README_TXT)));
    }

    @Test
    public void canFetchPrefs() {
        String d = BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull();
        assertNotNull(d);
    }

    @Test
    public void canEstablishStorageLocation() throws IOException {
        BriefcasePreferences bp = mock(BriefcasePreferences.class);
        final Path tempPath = Files.createTempDirectory("xxxx");
        when(bp.getBriefcaseDirectoryOrNull()).thenReturn(tempPath.toString());

        UiStateChangeListener uscl = mock(UiStateChangeListener.class);
        StorageLocation sl = new StorageLocation(bp);

        sl.establishBriefcaseStorageLocation(null, uscl);
        verify(uscl, times(1)).setFullUIEnabled(true);
    }
}
