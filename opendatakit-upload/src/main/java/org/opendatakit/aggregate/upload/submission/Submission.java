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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;

/**
 * Submission represents a single submission destined for a specific ODK Aggregate server.
 * A single submission is a folder which contains a filled out xform and accompanying files.
 * 
 * @author dylan@cs.washington.edu
 *
 */
public class Submission implements Callable<SubmissionResult>
{
	public static final String EXTENSION_XML =".xml";
	public static final String EXTENSION_PNG = ".png";
	public static final String EXTENSION_JPG = ".jpg";
	public static final String EXTENSION_JPEG = "jpeg";
	
	public static final String MIME_XML = "text/xml";
	public static final String MIME_PNG = "image/png";
	public static final String MIME_JPEG = "image/jpeg";
	
	public static final String FORM_PART_XML_SUBMISSION_FILE = "xml_submission_file";
	
	private final HttpClient _httpClient;
	private final URL _aggregateURL;
	private final File _submissionDir;
	private Logger _logger;

	/**
	 * Construct a Submission using the given http client, aggregate url,
	 * submission dir, and logger.
	 * 
	 * @param httpClient an org.apache.http.client.HttpClient
	 * @param aggregateURL
	 * @param submissionDir
	 * @param logger
	 */
	Submission(HttpClient httpClient, URL aggregateURL, File submissionDir, Logger logger)
	{
		_httpClient = httpClient;
		_aggregateURL = aggregateURL;
		_submissionDir = submissionDir;
		_logger = logger;
	}

	@Override
	public SubmissionResult call() 
	{
        SubmissionResult result = new SubmissionResult(_submissionDir, _aggregateURL, false);

        // prepare response and return uploaded
        String response = null;
        try 
        {
    		HttpPost httppost = buildSubmissionPost(_aggregateURL, _submissionDir);
        	ResponseHandler<String> handler = new BasicResponseHandler();
            response = _httpClient.execute(httppost, handler);
        } 
        catch (Exception e)
        {
        	getLogger().severe(Arrays.toString(e.getStackTrace()));
        	getLogger().severe(e.toString());
        	result.setSuccess(false);
        	result.setFailureReason(e.toString());
        	return result;
        }
//        catch (HttpResponseException e)
//        {
//        	getLogger().severe(Arrays.toString(e.getStackTrace()));
//        	getLogger().severe(e.getMessage());
//        	result.setSuccess(false);
//        	return result;
//        }
//        catch (ClientProtocolException e) 
//        {
//        	getLogger().severe(Arrays.toString(e.getStackTrace()));
//        	getLogger().severe(e.getMessage());
//        	result.setSuccess(false);
//        	return result;
//        } 
//        catch (IOException e) 
//        {
//        	getLogger().severe(Arrays.toString(e.getStackTrace()));
//        	getLogger().severe(e.getMessage());
//        	result.setSuccess(false);
//        	return result;
//        } catch (URISyntaxException e) {
//        	getLogger().severe(Arrays.toString(e.getStackTrace()));
//        	getLogger().severe(e.getMessage());
//        	result.setSuccess(false);
//        	return result;
//		} catch (Exception e)
//		{
//        	getLogger().severe(Arrays.toString(e.getStackTrace()));
//        	getLogger().severe(e.getMessage());
//        	result.setSuccess(false);
//        	return result;
//		}

        getLogger().info("Response:" + response);
        result.setSuccess(response != null);
        return result;        
	}

	/**
	 * Returns an org.apache.http.client.methods.HttpPost built to post to the
	 * given url. Builds a post that ODK Aggregate will recognize as a
	 * submission.
	 * 
	 * @param url
	 *            the URL of the ODK Aggregate submission servlet
	 * @param submissionDir
	 *            the submission, i.e. a File representing a directory
	 *            containing submission files
	 * @return an HttpPost
	 * @throws URISyntaxException
	 *             if the given url could not be converted to a URI for the
	 *             HttpPost
	 * @throws IllegalArgumentException
	 *             if the submissionDir is invalid, i.e. it has more than one
	 *             XML file
	 */
	protected HttpPost buildSubmissionPost(URL url, File submissionDir) throws URISyntaxException
	{
		URI uri = url.toURI();
		HttpPost post = new HttpPost(uri);
		boolean seenXML = false;

		// mime post
        MultipartEntity entity = new MultipartEntity();
        for (File f : submissionDir.listFiles()) 
        {
            if (f.getName().endsWith(EXTENSION_XML)) 
            {
            	if (seenXML)
            	{
            		throw new IllegalArgumentException(String.format("submissionDir (%s) has more than one xml file!", submissionDir));
            	}
            	seenXML = true;
                entity.addPart(FORM_PART_XML_SUBMISSION_FILE, new FileBody(f, MIME_XML));
                getLogger().info("added xml file " + f.getName());
            } 
            else if (f.getName().endsWith(EXTENSION_PNG)) 
            {
                entity.addPart(f.getName(), new FileBody(f, MIME_PNG));
                getLogger().info("added image file " + f.getName());
            } 
            else if (f.getName().endsWith(EXTENSION_JPG) || f.getName().endsWith(EXTENSION_JPEG)) 
            {
                entity.addPart(f.getName(), new FileBody(f, MIME_JPEG));
                getLogger().info("added image file " + f.getName());
            } 
            else 
            {
                getLogger().warning("unsupported file type, not adding file: " + f.getName());
            }
        }
        if (!seenXML)
        {
        	throw new IllegalArgumentException(String.format("submissionDir (%s) has no xml file!", submissionDir));
        }
        post.setEntity(entity);
        return post;
	}
	
	private Logger getLogger() {
		if (_logger == null)
		{
			Logger logger = Logger.getLogger(this.getClass().getName());
			logger.setLevel(Level.OFF);
			_logger = logger;
		}
			return _logger;
	}
}
