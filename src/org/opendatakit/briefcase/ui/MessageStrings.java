/*
 * Copyright (C) 2012 University of Washington.
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

package org.opendatakit.briefcase.ui;

import org.opendatakit.briefcase.util.FileSystemUtils;

public class MessageStrings {

  public static final String BRIEFCASE_STORAGE_LOCATION =
      "ODK Briefcase Storage Location";
  public static final String ODK_DIRECTORY =
      "ODK Directory";
  public static final String EXPORT_DIRECTORY =
      "Export Directory";

  public static final String DIR_NOT_EXIST =
      "Directory does not exist";
  public static final String DIR_NOT_DIRECTORY =
      "You must specify a directory";
  public static final String DIR_INSIDE_BRIEFCASE_STORAGE = 
      "Directory appears to be nested within an enclosing ODK Briefcase Storage area";
  public static final String DIR_INSIDE_ODK_DEVICE_DIRECTORY =
      "The directory you have chosen appears to be within an ODK Device directory";
  public static final String DIR_NOT_ODK_COLLECT_DIRECTORY =
      "Not an ODK Collect directory (did not find both the forms and instances child directories)";
  public static final String INVALID_ODK_DIRECTORY =
      "Invalid " + ODK_DIRECTORY;
  public static final String INVALID_BRIEFCASE_STORAGE_LOCATION =
      "Invalid " + BRIEFCASE_STORAGE_LOCATION;
  public static final String INVALID_EXPORT_DIRECTORY =
      "Invalid " + EXPORT_DIRECTORY;
  public static final String INVALID_DIRECTORY =
      "Invalid Directory";
  public static final String INVALID_PEM_FILE =
      "Invalid Private Key (PEM) File";
  
  public static final String INVALID_PEM_FILE_DIALOG_TITLE =
      "Invalid Private Key (PEM) File";
  public static final String BRIEFCASE_STORAGE_LOCATION_DIALOG_TITLE = 
      BRIEFCASE_STORAGE_LOCATION;
  public static final String BRIEFCASE_STORAGE_LOCATION_EXPLANATION_HTML =
      "<body><font face=\"verdana,sans-serif\" size=\"+1\"><p>Please " +
      "specify the location of the <em>" + FileSystemUtils.BRIEFCASE_DIR +
      "</em> area.</p>" +
      "<p>ODK Briefcase uses a storage area named <em>" +
          FileSystemUtils.BRIEFCASE_DIR + 
    "</em> identified by the " + BRIEFCASE_STORAGE_LOCATION +
    ". This storage area holds all " +
    "your form and submission data.</p><p>" +
    "Once created, you can copy and transport this storage area " +
    "across systems, just like a briefcase of paper documents.</p></font></body>";
  public static final String README_CONTENTS =
      "This ODK Briefcase storage area retains\n" +
      "all the forms and submissions that have been\n" +
      "gathered into it.\n" +
      "\n" +
      "Users should not navigate into or modify its\n" +
      "contents unless explicitly directed to do so.\n";

  public static final String ERROR_DIALOG_TITLE =
      "ODK Briefcase Action Failed";
  
  public static final String PROXY_HOST = "Host";
  public static final String PROXY_PORT = "Port";
  public static final String PROXY_SCHEMA = "Schema";


}
