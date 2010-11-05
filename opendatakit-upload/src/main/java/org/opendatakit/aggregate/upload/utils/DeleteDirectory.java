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

public class DeleteDirectory {

	/**
	 * Deletes the given file. Useful for recursively deleting non-empty
	 * directories.
	 * 
	 * @param path
	 *            the File to delete
	 * @return true if the File was successfully deleted, false otherwise.
	 */
	public static boolean deleteDirectory(File path) 
	{
		if(path.exists()) 
		{
	      File[] files = path.listFiles();
	      for(int i=0; i < files.length; i++) 
	      {
	         if(files[i].isDirectory()) 
	         {
	           deleteDirectory(files[i]);
	         }
	         else 
	         {
	           files[i].delete();
	         }
	      }
	    }
	    return(path.delete());
	}
}
