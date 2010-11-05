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
package org.opendatakit.aggregate.upload.submission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.aggregate.upload.test.TestUtilities;

public class SubmissionUploaderTest {

	private static final Random _rand = new Random();
	
	private SubmissionUploader _uploader;
	
	@Before
	public void setUp()
	{
		HttpClient mockHttpClient = TestUtilities.getMockHttpClient("non-null response");
        ExecutorService es = new ThreadPoolExecutor(0, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		_uploader = new SubmissionUploader(mockHttpClient, es);
		_uploader.getLogger().setLevel(Level.OFF);
	}
	
	// Is this a bad test because it relies on the Submission class?
	@Test
	public void testUploadSubmissionWithTwoGoodSubmissions() throws MalformedURLException, InterruptedException, ExecutionException
	{
		testUploadSubmissions(1, 2, "non_null_submission.xml");
	}
	
	@Test
	public void testUploadSubmissionWithTwoBadSubmissions()
	{
		testUploadSubmissions(1, 2, "");
	}
	
	public void testUploadSubmissions(int submissionCountStart, int submissionCountEnd, String submissionXMLName)
	{
		try
		{
	        String submissionPrefix = "submission_";
	        String pic = "pic.jpg";
	        File submissionsParentDir = TestUtilities.buildSubmissionSet(submissionCountStart, submissionCountEnd, submissionPrefix, submissionXMLName, pic);
			URL url = new URL("http://nowhere.com");
						
			List<Future<SubmissionResult>> futures = _uploader.uploadSubmissions(submissionsParentDir, url);
			for (Future<SubmissionResult> future : futures)
			{
				SubmissionResult result = future.get();
				File submissionDir = result.getFile();
				assertTrue(submissionDir.getName().startsWith(submissionPrefix));
				List<String> files = Arrays.asList(submissionDir.list());
				assertEquals(!submissionXMLName.equals(""), files.contains(submissionXMLName));
				assertTrue(files.contains(pic));
				assertEquals(!submissionXMLName.equals(""), result.isSuccess());
			}
		}
		catch (Exception e)
		{
			fail(e.toString());
		}
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithAllSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(100);
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithNoSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(0);
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithHalfSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(50);
	}
	
	private void testCheckSubmissionResultsAndDeleteSubmissions(int successProb)
	{
		String submissionPrefix = "submission";
		String submissionXML = "submission.xml";
		String[] otherFiles = new String[]{"pic1.jpg", "pic2.png"};
		File submissionParentDir = TestUtilities.buildSubmissionSet(1, 10, submissionPrefix, submissionXML, otherFiles);
		List<Future<SubmissionResult>> results = buildSubmissionResultSet(submissionParentDir, successProb);
		_uploader.checkSubmissionResultsAndDeleteSubmissions(results);
		for (Future<SubmissionResult> future : results)
		{
			try
			{
				SubmissionResult result = future.get();
				boolean success = result.isSuccess();
				if (successProb == 100)
					assertTrue(success);
				else if (successProb == 0)
					assertTrue(!success);
				
				if (success)
					assertTrue(!result.getFile().exists());
				else
					assertTrue(result.getFile().exists());
			}
			catch (Exception e)
			{
				fail(e.toString());
			}
		}
	}

	/**
	 * 
	 * @param submissionsParentDir the parent dir containing all of the submission directories under it.
	 * @param probOfSuccess percentage probability of the SubmissionResult.isSuccess() returning true (out of 100%).
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Future<SubmissionResult>> buildSubmissionResultSet(File submissionsParentDir, int probOfSuccess)
	{
		List<Future<SubmissionResult>> futures = new ArrayList<Future<SubmissionResult>>();
		File[] submissionFolders = submissionsParentDir.listFiles(new FileFilter() {
				
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
       	for (File submissionFolder : submissionFolders)
       	{
       		int newInt = _rand.nextInt(100);
       		boolean success = (newInt <= (probOfSuccess - 1));
			SubmissionResult result = new SubmissionResult(submissionFolder, null, success);
			Future<SubmissionResult> mockFuture = mock(Future.class);
			try
			{
				when(mockFuture.get()).thenReturn(result);
			}
			catch (Exception e)
			{
				fail(e.toString());
			}
			futures.add(mockFuture);
       	}
		return futures;
	}
}
