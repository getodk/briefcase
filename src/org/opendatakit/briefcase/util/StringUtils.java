/*
 * Copyright (C) 2017 University of Washington.
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

package org.opendatakit.briefcase.util;

/**
 * Common utility methods to handle extended String functionalities.
 * 
 * @author rclakmal@gmail.com
 * 
 */
public class StringUtils {
	
	public static final EMPTY_STRING = "";
	
	/**
	 * returns true if and only if s is neither null nor empty
	*/
	public static boolean isNotEmptyNotNull(String s) {
		return s != null && !s.isEmpty();
	}
	
	/**
	 * returns true if s is null, or empty, or if s is only whitespace (), such as for example " ".
	*/
	public static boolean isWhiteSpace(String s) {
		if(s == null || s.isEmpty()){
			return true;
		}
		String trimmedString = s.trim();
		return trimmedString.isempty();
	}
	
	/**
	 * null safe check for equality of two different strings.
	 *
	 * returns true if and only if the strings are identical.
	*/
	public static boolean areEqualStrings(String s1, String s2) {
		if(s1 == null && s2 == null){
			return true;
		}
		if((s1 == null && s2 != null) || (s1 != null && s2 == null)){
			return false;
		}
		return s1.equals(s2);
	}
}
