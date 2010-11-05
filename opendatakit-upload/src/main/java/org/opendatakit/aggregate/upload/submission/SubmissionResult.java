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
import java.net.URL;

/**
 * A SubmissionResult represents the result of an attempt to submit a Submission.
 * This is what will be returned as the result from Submission.call().
 * 
 * @author dylan
 *
 */
public class SubmissionResult {

	private static final String DEFAULT_FAILURE_REASON = "Unknown";
	
	private File _submissionFile;
	private boolean _success;
	private URL _aggregateURL;
	private String _reason;
	
	/**
	 * Construct a new SubmissionResult.
	 * 
	 * @param submissionFile the File (should be a directory) associated with this Submission.
	 * @param submissionURL the URL the Submission was sent to.
	 * @param success whether the Submission was successful or not.
	 */
	public SubmissionResult(File submissionFile, URL submissionURL, boolean success)
	{
		_submissionFile = submissionFile;
		_aggregateURL = submissionURL;
		_success = success;
		_reason = DEFAULT_FAILURE_REASON;
	}
	
	public URL getAggregateURL() {
		return _aggregateURL;
	}

	public void setAggregateURL(URL aggregateURL) {
		_aggregateURL = aggregateURL;
	}

	public File getFile()
	{
		return _submissionFile;
	}
	
	public boolean isSuccess()
	{
		return _success;
	}
	
	public void setFile(File submissionFile)
	{
		_submissionFile = submissionFile;
	}
	
	public void setSuccess(boolean success)
	{
		_success = success;
	}
	
	public void setFailureReason(String reason)
	{
		_reason = reason;
	}
	
	public String getFailureReason()
	{
		return _reason;
	}
}
