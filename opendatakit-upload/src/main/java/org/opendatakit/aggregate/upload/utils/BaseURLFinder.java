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

import java.net.MalformedURLException;
import java.net.URL;

public class BaseURLFinder {
	
  private static final char SLASH = '/';
  
	/**
	 * Returns the base url of the given url. 
	 * http://www.google.com 				--> http://www.google.com
	 * http://www.google.com/something 		--> http://www.google.com
	 * https://www.google.com:80/?p=param	--> https://www.google.com:80
	 * http://www.google.com/something/else --> http://www.google.com/something
	 * 
	 * @param url the URL to find the base for
	 * @return the base URL
	 * @throws MalformedURLException 
	 */
	public static URL getBaseURL(URL url) throws MalformedURLException
	{
	   String baseURLString = url.toString();
		int slashCount = 0;
	   for (char c : baseURLString.toCharArray())
		{
		  if (c == SLASH)
		  {
		    slashCount++;
		  }
		}
	   // check if there are more slashes than in http://
	   if (slashCount > 2)
	   {

    	   int index = baseURLString.lastIndexOf(SLASH, baseURLString.length() - 1);
    		baseURLString = baseURLString.substring(0, index);
	   }
		return new URL(baseURLString);
	}
}
