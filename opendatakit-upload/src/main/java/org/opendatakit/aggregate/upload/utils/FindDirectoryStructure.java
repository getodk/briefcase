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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FindDirectoryStructure 
{	
	private static final String PROPERTY_OS = "os.name";
	private static final String OS_WINDOWS = "Windows";
	private static final String OS_MAC = "Mac";
	
	private static final String[] MAC_MOUNTS = 
		new String[]{"/Volumes", "/media", "/mnt"};
	private static final String[] UNIX_MOUNTS = 
		new String[]{"/mnt", "/media"};

	/**
	 * Searches mounted drives for the given directory structure and returns a
	 * list of positive results. Works for Windows, Mac, and Linux operating
	 * systems.
	 * 
	 * @param directoryStructureToSearchFor
	 *            the directory structure to search for
	 * @return a List<File> containing matches of currently mounted file systems
	 *         which contain the directoryStructureToSearchFor
	 */
	public static List<File> searchMountedDrives(String directoryStructureToSearchFor)
	{
		directoryStructureToSearchFor = normalizePathString(directoryStructureToSearchFor);
		String os = System.getProperty(PROPERTY_OS);
		if (os.contains(OS_WINDOWS))
		{
			File[] drives = File.listRoots();
			List<String> driveStrings = new ArrayList<String>();
			if (drives != null)
			{
				for (File root : drives)
				{
					driveStrings.add(root.getAbsolutePath());
				}
				return search(driveStrings, directoryStructureToSearchFor);
			}
		}
		else if (os.contains(OS_MAC))
		{
			List<String> driveStrings = new ArrayList<String>();
			for (String mountPointString : MAC_MOUNTS)
			{
				File mountPoint = new File(mountPointString);
				if (mountPoint != null)
				{
					File[] mounts = mountPoint.listFiles();
					if (mounts != null)
					{
						for (File driveString : mounts)
						{
							driveStrings.add(driveString.getAbsolutePath());
						}
					}
				}
			}
			return search(driveStrings, directoryStructureToSearchFor);
		}
		else // Assume Unix
		{
			List<String> driveStrings = new ArrayList<String>();
			for (String mountPointString : UNIX_MOUNTS)
			{
				File mountPoint = new File(mountPointString);
				if (mountPoint != null)
				{
					File[] mounts = mountPoint.listFiles();
					if (mounts != null)
					{
						for (File driveString : mountPoint.listFiles())
						{
							driveStrings.add(driveString.getAbsolutePath());
						}
					}
				}
			}
			return search(driveStrings, directoryStructureToSearchFor);
		}
		return new ArrayList<File>();
	}

	/**
	 * Checks each given potential directory for existence of the given
	 * directory structure under it and returns a list of positive matches.
	 * 
	 * @param potentialDirectories
	 *            the potential directories to check.
	 * @param directoryStructure
	 *            the directory structure to search for under each potential
	 *            directory
	 * @return a List<File> containing potentialDirectories which contained the
	 *         given directory structure underneath them
	 */
	public static List<File> search(List<String> potentialDirectories, String directoryStructure)
	{
		ArrayList<File> potentialMatches = new ArrayList<File>();
		for (String potentialTopLevelDirectory : potentialDirectories)
		{
			File odkInstancesDir = new File(potentialTopLevelDirectory + directoryStructure);
			if (odkInstancesDir != null && odkInstancesDir.exists())
			{
				potentialMatches.add(odkInstancesDir);
			}
		}
		return potentialMatches;
	}

	/**
	 * Takes a string representing a path to a file or directory and normalizes
	 * it to use the path separator specified in File.separator.
	 * 
	 * @param pathString
	 *            a String holding the path to normalize
	 * @return a normalized path using File.separator
	 */
	public static String normalizePathString(String pathString)
	{
		int unixIndex = pathString.indexOf("/");
		int windowsIndex = pathString.indexOf("\\");
		if (unixIndex != -1)
		{
			pathString = pathString.replace("/", File.separator);
		}
		if (windowsIndex != -1)
		{
			pathString = pathString.replace("\\", File.separator);
		}
		return pathString;
	}
}
