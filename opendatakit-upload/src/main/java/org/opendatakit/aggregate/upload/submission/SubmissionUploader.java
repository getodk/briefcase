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

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.opendatakit.aggregate.upload.utils.DeleteDirectory;

/**
 * Manages the process of submitting a group of submissions to an ODK Aggregate
 * server. Using a parent directory containing multiple submissions (e.g.
 * sdcard/odk/instances on an Android phone running ODK Collect), handles
 * uploading submissions, deleting successful submissions, and reporting
 * unsuccessful submissions back to the caller.
 * 
 * @author dylan
 * 
 */
public class SubmissionUploader 
{
	public static String LOGGER = "SubmissionUploader";
	
	private ExecutorService _executorService;
	private Logger _logger;
	private HttpClient _httpClient;
	
	/**
	 * Constructs the SubmissionUploader using the given http client and executor service.
	 * 
	 * @param httpClient the org.apache.http.client.HttpClient to use.
	 * @param executorService the ExecutorService to use.
	 */
	public SubmissionUploader(HttpClient httpClient, ExecutorService executorService)
	{
		_executorService = executorService;
		_httpClient = httpClient;
		
        // Set up Logger
        Logger logger = Logger.getLogger(LOGGER);
        logger.setLevel(Level.ALL);
        _logger = logger;
	}

	/**
	 * Uploads the given directory containing submission folders to the given
	 * aggregate instance and returns a list of results.
	 * 
	 * @param submissionsParentDir
	 *            a directory containing a set of subdirectories, each
	 *            containing a single submission
	 * @param submissionURL
	 *            the URI to post the submissions to (aggregate servlet)
	 * @return a List<Future<SubmissionResults>> representing the future result
	 *         of each submission
	 */
	public List<Future<SubmissionResult>> uploadSubmissions(File submissionsParentDir, URL submissionURL)
	{
        // loop through submissions
        File[] submissionFolders = submissionsParentDir.listFiles(new FileFilter() {
			
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
        List<Future<SubmissionResult>> futures = new ArrayList<Future<SubmissionResult>>();
        for (File submissionFolder : submissionFolders)
        {
        	Submission submission = new Submission(_httpClient, submissionURL, submissionFolder, _logger);
        	Future<SubmissionResult> future = _executorService.submit(submission);
        	futures.add(future);        	
        }
        return futures;
	}

	/**
	 * Check that the submissions were uploaded successfully, and delete each
	 * successfully uploaded submission. Returns a list of submissions which
	 * were not uploaded successfully.
	 * 
	 * @param submissionResults
	 *            the List<Future<SubmissionResults>> obtained from calling
	 *            uploadSubmissions.
	 * @param logger
	 *            the logger to use for logging
	 * @return a List<SubmissionResult> containing the submissions which were
	 *         not successful
	 */
	public List<SubmissionResult> checkSubmissionResultsAndDeleteSubmissions(List<Future<SubmissionResult>> submissionResults)
	{
		List<SubmissionResult> failedSubmissions = new ArrayList<SubmissionResult>();
		for (Future<SubmissionResult> futureResult : submissionResults)
		{
			SubmissionResult result = null;
			try 
			{
				result = futureResult.get();
			} catch (InterruptedException e) 
			{
				getLogger().warning(Arrays.toString(e.getStackTrace()));
				getLogger().warning(e.getMessage());
			} catch (ExecutionException e) 
			{
				getLogger().warning(Arrays.toString(e.getStackTrace()));
				getLogger().warning(e.getMessage());
			}
			if (result != null && !result.isSuccess())
			{
				failedSubmissions.add(result);
				getLogger().warning("Submission failed: " + result.getFile() + ". Reason: " + result.getFailureReason());
			}
			else
			{
				File uploadedFile = result.getFile();
				try
				{
					boolean deleted = DeleteDirectory.deleteDirectory(uploadedFile);
					if (!deleted)
					{
						result.setFailureReason("Successful upload but unable to delete submission.");
						failedSubmissions.add(result);
						getLogger().warning("Successful upload but unable to delete file: " + uploadedFile);
					}
				}
				catch (SecurityException e)
				{
					result.setFailureReason(e.getMessage());
					failedSubmissions.add(result);
					getLogger().warning("Unable to delete file: " + uploadedFile);
				}
			}
		}
		return failedSubmissions;
	}
	
	/**
	 * Returns the logger associated with the SubmissionUploader. Guaranteed to
	 * return a non-null Logger.
	 * 
	 * @return the Logger associated with the SubmissionUploader.
	 */
	public Logger getLogger() {
		if (_logger == null)
		{
			Logger logger = Logger.getLogger(LOGGER);
			logger.setLevel(Level.OFF);
			_logger = logger;
		}
			return _logger;
	}

	public static void main(String[] args)
	{
		if (args.length == 2)
		{
			String submissionsParentDirString = args[0];
			String aggregateURLString = args[1];
			if (aggregateURLString.endsWith("/"))
				aggregateURLString = aggregateURLString.substring(0, aggregateURLString.length() - 1);
			
			File submissionsParentDir = new File(submissionsParentDirString);
			URL aggregateURL = null;
			
			try 
			{
				aggregateURL = new URL(aggregateURLString + "/submission");
			} 
			catch (MalformedURLException e) 
			{
				System.out.println("Bad URL:");
				System.out.println(e.getMessage());
			}
			System.out.println("Using submission dir: " + submissionsParentDir);
			System.out.println("Using aggregateURL: " + aggregateURL);
	        
			// configure connection
	        HttpParams params = new BasicHttpParams();
	        HttpConnectionParams.setConnectionTimeout(params, 30000);
	        HttpConnectionParams.setSoTimeout(params, 30000);
	        HttpClientParams.setRedirecting(params, false);

	        // setup client
	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme(aggregateURL.getProtocol(), PlainSocketFactory.getSocketFactory(), aggregateURL.getPort()));
	        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
	        HttpClient httpClient = new DefaultHttpClient(manager, params);
	
		    // Set up ExecutorService
		    ExecutorService es = new ThreadPoolExecutor(0, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			
			SubmissionUploader uploader = new SubmissionUploader(httpClient, es);
			uploader.uploadSubmissions(submissionsParentDir, aggregateURL);
		}
		else
		{
			System.out.println("Usage: SubmissionUploader <submissionsParentDir> <aggregateURL>");
			System.out.println("<submissionsParentDir>: a directory containing multiple directories, each one corresponding to a submission.");
			System.out.println("<aggregateURL>: the location of the aggregate instance to hit, e.g. 'http://localhost:8080/");
			System.out.println();
			System.out.println("Note: It is assumed that you have already uploaded the appropriate form definitions for the submissions.");
		}
	}
}
