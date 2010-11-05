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
package org.opendatakit.aggregate.upload.test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Ignore;

@Ignore
public class TestUtilities {
	
	public static File createTemporaryDirectory()
	{
		try
		{
			File tempDir = File.createTempFile("temp", Long.toString(System.nanoTime()));
		    if(!(tempDir.delete()))
		    {
		        throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
		    }
	
		    if(!(tempDir.mkdir()))
		    {
		        throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
		    }
		    return tempDir;
		}
		catch (IOException e)
		{
			fail(e.toString());
		}
		return null;
	}

	/**
	 * Same as buildSubmission, except a new temporary directory is
	 * automatically created to hold the submission and the file returned is the
	 * submission directory instead of the parent dir.
	 * 
	 * @param submissionName
	 *            the name of the folder to contain the submission
	 * @param submissionXMLName
	 *            the name of the XML submission file (should end in .xml) -
	 *            ends up in "submissionName/submissionXMLName".
	 * @param otherFileNames
	 *            names of other files to create in the submission dir (e.g.
	 *            "pic.jpg", "randomfile.exe")
	 * @return a File representing the newly created submission
	 * @throws IOException
	 */
	public static File buildSingleSubmission(String submissionName, String submissionXMLName, String... otherFileNames)
	{
		File parentDirectory = buildSubmission(createTemporaryDirectory(), submissionName, submissionXMLName, otherFileNames);
		return new File(parentDirectory.getAbsolutePath() + File.separator + submissionName);
	}
	
	/**
	 * Builds a submission under the specified parentDirectory.
	 * 
	 * @param parentDirectory
	 *            the directory to create the submission under
	 * @param submissionName
	 *            the name of the folder to contain the submission
	 * @param submissionXMLName
	 *            the name of the XML submission file (should end in .xml) -
	 *            ends up in "submissionName/submissionXMLName".
	 * @param otherFileNames
	 *            names of other files to create in the submission dir (e.g.
	 *            "pic.jpg", "randomfile.exe")
	 * @return the parentDirectory
	 * @throws IOException
	 */
	public static File buildSubmission(File parentDirectory, String submissionName, String submissionXMLName, String... otherFileNames)
	{
		try
		{
			if (!parentDirectory.exists())
			{
				throw new IllegalArgumentException("parentDirectory: " + parentDirectory + " must exist already.");
			}
			else
			{
				File submissionFolder = new File(parentDirectory.getAbsolutePath() + File.separator + submissionName);
				submissionFolder.mkdir();
				File submissionXMLFile = new File(submissionFolder.getAbsolutePath() + File.separator + submissionXMLName);
				submissionXMLFile.createNewFile();
				for (String otherFileName : otherFileNames)
				{
					File otherFile = new File(submissionFolder.getAbsolutePath() + File.separator + otherFileName);
					otherFile.createNewFile();
				}
			}
		}
		catch (IOException e)
		{
			fail(e.toString());
		}
		return parentDirectory;
	}

	/**
	 * Builds a set of submissions under a freshly created temporary directory.
	 * Returns this directory, the submissions parent directory (i.e. the
	 * directory containing all the submission directories)
	 * 
	 * @param submissionCountStart the start of the count for submissions
	 * @param submissionCountEnd the end of the count for submissions
	 * @param submissionNamePrefix the prefix to use to name all the submissions (submission names go from 'submissionPrefix + submissionCountStart' --> 'submissionPrefix + submissionCountEnd')
	 * @param submissionXMLName the name of the submission XML file to create in each submission (should end in .xml)
	 * @param otherFileNames other files to create under each submission directory
	 * @return a File representing the submission parent directory
	 */
	public static File buildSubmissionSet(int submissionCountStart, int submissionCountEnd, String submissionNamePrefix, String submissionXMLName, String... otherFileNames)
	{
		File tempDir = TestUtilities.createTemporaryDirectory();
		for (int i = submissionCountStart; i <= submissionCountEnd; i++)
		{
			TestUtilities.buildSubmission(tempDir, submissionNamePrefix + i, submissionXMLName, otherFileNames);
		}
		return tempDir;
	}

	/**
	 * Returns a mock HttpClient, which returns the given response when
	 * HttpClient.execute(HttpUriRequest, ResponseHandler) is called
	 * 
	 * @param response
	 *            the response to return
	 * @return a mock HttpClient
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@SuppressWarnings("unchecked")
	public static HttpClient getMockHttpClient(String response)
	{
		HttpClient mockHttpClient = null;
		try
		{
			mockHttpClient = mock(HttpClient.class);
			when(mockHttpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class))).thenReturn(response);
		}
		catch(Exception e)
		{
			fail(e.toString());
		}
		return mockHttpClient;
	}
}
