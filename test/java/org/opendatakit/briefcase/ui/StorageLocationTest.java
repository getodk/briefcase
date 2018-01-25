/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.ui;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;

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
