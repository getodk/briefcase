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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.opendatakit.aggregate.upload.test.TestUtilities;

public class DeleteDirectoryTest {
	
	@Test
	public void testDeleteDirectoryWithOneFile() throws IOException
	{
		File tempDir = TestUtilities.createTemporaryDirectory();
		File subFile = new File(tempDir.getAbsolutePath() + "/someFile");
		subFile.createNewFile();
		
		assertTrue(DeleteDirectory.deleteDirectory(tempDir));
		assertTrue(!tempDir.exists());
	}
	
	@Test
	public void testDeleteDirectoryWithOneFolder() throws IOException
	{
		File tempDir = TestUtilities.createTemporaryDirectory();
		File subDir = new File(tempDir.getAbsolutePath() + "/someFolder");
		subDir.mkdir();
		
		assertTrue(DeleteDirectory.deleteDirectory(tempDir));
		assertTrue(!tempDir.exists());
	}
	
	@Test
	public void testDeleteDirectoryWithNestedStructure() throws IOException
	{
		File tempDir = TestUtilities.createTemporaryDirectory();
		String tempDirPath = tempDir.getAbsolutePath();
		for (int i = 1; i < 5; i++)
		{
			for (int j = 1; j < 5; j++)
			{
				String deepSubDirString = String.format("/someFolder%d/someFolder%d/someFolder", i, j);
				File deepSubDir = new File(tempDirPath + deepSubDirString);
				deepSubDir.mkdirs();
				File deepFile = new File(tempDirPath + deepSubDirString + "/somFile");
				deepFile.createNewFile();
			}
		}
		
		assertTrue(DeleteDirectory.deleteDirectory(tempDir));
		assertTrue(!tempDir.exists());
	}
}
