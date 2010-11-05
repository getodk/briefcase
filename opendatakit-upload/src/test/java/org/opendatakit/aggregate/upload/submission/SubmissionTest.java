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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.aggregate.upload.test.TestUtilities;

public class SubmissionTest 
{
	private static final String VARIABLE_NAME = "${name}";
	private static final String VARIABLE_FILENAME = "${filename}";
	private static final String VARIABLE_MIMETYPE = "${mimetype}";
	private static final String CONTENT_DISPOSITION = String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", VARIABLE_NAME, VARIABLE_FILENAME);
	private static final String CONTENT_TYPE = String.format("Content-Type: %s", VARIABLE_MIMETYPE);
	
	private Submission _submission;
	
	@Before
	public void setup() throws URISyntaxException, MalformedURLException
	{
		_submission = new Submission(null, null, null, null);
	}
	
	@Test
	public void testCallWithNonNullResponse()
	{
		testCall("non null response");
	}
	
	@Test
	public void testCallWithNullResponse()
	{
		testCall(null);
	}
	
	private void testCall(String response)
	{
		try
		{
			HttpClient mockHttpClient = TestUtilities.getMockHttpClient(response);
			URL url = new URL("http://nowhere.com");
			File submissionDir = TestUtilities.buildSingleSubmission("submission", "submission.xml", "pic1.jpg");
			Submission submission = new Submission(mockHttpClient, url, submissionDir, null);
			SubmissionResult result = submission.call();
			if (response != null)
			{
				assertTrue(result.isSuccess());
			}
			else
			{
				assertTrue(!result.isSuccess());
			}
			assertEquals(url, result.getAggregateURL());
		}
		catch(IOException e)
		{
			fail(e.toString());
		}
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXML()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXML", "submission.xml");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpg()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpg");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpeg()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpeg");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOnePng()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.png");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpgOneJpegOnePng()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpg", "pic2.jpeg", "pic3.png");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneHTML()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithOneXMLOneHTML", "submission.xml", "shouldBeIgnored.html");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBuildSubmissionPostWithTwoXML()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithTwoXML", "submission1.xml", "throwsIllegalArgumentException.xml");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBuildSubmissionPostWithNoXML()
	{
		testBuildSubmissionPost("http://nowhere.com", "submissionWithNoXML", "");
	}
	
	private void testBuildSubmissionPost(String submissionURLString, String submissionName, String submissionFileName, String... otherFileNames)
	{
		ByteArrayOutputStream out = null;
		try 
		{
			URL submissionURL = new URL(submissionURLString);
			File submissionFolder = TestUtilities.buildSingleSubmission(submissionName, submissionFileName, otherFileNames);
			HttpPost post = _submission.buildSubmissionPost(submissionURL, submissionFolder);
			HttpEntity entity = post.getEntity();
			out = new ByteArrayOutputStream();
			entity.writeTo(out);
		}
		catch (MalformedURLException e) 
		{
			fail(e.getMessage());
		}
		catch (IOException e) 
		{
			fail(e.getMessage());
		} 
		catch (URISyntaxException e) 
		{
			fail(e.getMessage());
		} 
		String multipartEntity = out.toString();
		assertTrue(multipartEntity.contains(makeContentDispositionString(Submission.FORM_PART_XML_SUBMISSION_FILE, submissionFileName)));
		assertTrue(multipartEntity.contains(makeContentTypeString(Submission.MIME_XML)));
		for (String otherFileName : otherFileNames)
		{
			if (otherFileName.endsWith(Submission.EXTENSION_JPG) || otherFileName.endsWith(Submission.EXTENSION_JPEG))
			{
				assertTrue(multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
				assertTrue(multipartEntity.contains(makeContentTypeString(Submission.MIME_JPEG)));
			}
			else if (otherFileName.endsWith(Submission.EXTENSION_PNG))
			{
				assertTrue(multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
				assertTrue(multipartEntity.contains(makeContentTypeString(Submission.MIME_PNG)));				
			}
			else
			{
				assertTrue(!multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
			}
		}
	}
	
	private String makeContentDispositionString(String name, String filename)
	{
		return CONTENT_DISPOSITION.replace(VARIABLE_NAME, name).
			replace(VARIABLE_FILENAME, filename);		
	}
	
	private String makeContentTypeString(String mimetype)
	{
		return CONTENT_TYPE.replace(VARIABLE_MIMETYPE, mimetype);
	}
}
