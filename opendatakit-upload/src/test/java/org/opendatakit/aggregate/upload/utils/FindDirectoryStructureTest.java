/*
 * Copyright (C) 2010 University of Washington.
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
package org.opendatakit.aggregate.upload.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendatakit.aggregate.upload.test.TestUtilities;

public class FindDirectoryStructureTest {
	
	private String _directoryStructure = "/odk/instances";
	
	@Test
	public void testSearchForSubmissionParentDirs() throws IOException
	{
		// Set up ODK Collect submissions structure
		File tempMountDir = TestUtilities.createTemporaryDirectory();
		File tempSubmissionsParentDir = new File(tempMountDir.getAbsolutePath() + _directoryStructure);
		tempSubmissionsParentDir.mkdirs();
		
		// Set up more without the ODK submissions structure
		File tempMountDir2 = TestUtilities.createTemporaryDirectory();
		File tempMountDir3 = TestUtilities.createTemporaryDirectory();
		
		List<String> fakeMountPointsToSearch = new ArrayList<String>();
		fakeMountPointsToSearch.add(tempMountDir.getAbsolutePath());
		fakeMountPointsToSearch.add(tempMountDir2.getAbsolutePath());
		fakeMountPointsToSearch.add(tempMountDir3.getAbsolutePath());
		
		List<File> submissionsParentDirs = FindDirectoryStructure.search(fakeMountPointsToSearch, _directoryStructure);
		
		assertEquals(1, submissionsParentDirs.size());
		assertTrue(submissionsParentDirs.contains(tempSubmissionsParentDir));
	}
	
	@Test
	public void testNormalizePathStringGivenUnixPath()
	{
		String pathString = "/some/unix/path"; // has 3 path separators
		testNormalizePathString(pathString, 3);
	}
	
	@Test
	public void testNormalizePathStringGivenWindowsPath()
	{
		String pathString = "C:\\some\\windows\\path"; // has 3 path separators
		testNormalizePathString(pathString, 3);
	}
	
	@Test
	public void testNormalizePathStringGivenNonPathString()
	{
		String nonPathString = "some string";
		testNormalizePathString(nonPathString, 0);
	}
	
	private void testNormalizePathString(String pathString, int expectedSeparatorCount)
	{
		pathString = FindDirectoryStructure.normalizePathString(pathString);
		int actualSeparatorCount = 0;
		for (char c : pathString.toCharArray())
		{
			if (c == File.separatorChar)
			{
				actualSeparatorCount++;
			}
		}
		assertEquals(expectedSeparatorCount, actualSeparatorCount);
	}
}
