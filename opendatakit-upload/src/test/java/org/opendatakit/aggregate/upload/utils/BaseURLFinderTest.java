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
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class BaseURLFinderTest {
	@Test
	public void testGetBaseURLGivenBaseURL()
	{
		testSingleGetBaseURL("http://www.google.com", 
			"http://www.google.com");
	}
	
	@Test 
	public void testGetBaseURLGivenURLWithPath()
	{
		testSingleGetBaseURL("http://www.google.com/something",
			"http://www.google.com");
	}
	
	@Test
	public void testGetBaseURLGivenURLWithExtendedPath()
	{
		testSingleGetBaseURL("http://www.google.com/something/else/is/here",
			"http://www.google.com/something/else/is");
	}
	
	@Test
	public void testGetBaseURLGivenURLWithHttpsAndPortAndParam()
	{
		testSingleGetBaseURL("https://www.google.com:80/?p=param",
				"https://www.google.com:80");
	}
	
	private void testSingleGetBaseURL(String url, String base)
	{
		try
		{
			URL baseURL = new URL(base);
			URL urlToTest = new URL(url);
			assertEquals(baseURL, BaseURLFinder.getBaseURL(urlToTest));
		}
		catch (MalformedURLException e)
		{
			fail("Malformed URL: " + e.getMessage());
		}
	}
}
