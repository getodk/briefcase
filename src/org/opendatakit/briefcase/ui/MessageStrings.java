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

public class MessageStrings {

  public static final String BRIEFCASE_STORAGE_LOCATION =
      "Storage Location";
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
  public static final String INVALID_DATE_RANGE_MESSAGE = "Invalid date range: Start date must be before End date.";
  public static final String INVALID_DIRECTORY =
      "Invalid Directory";
  public static final String INVALID_PEM_FILE =
      "Invalid Private Key (PEM) File";

  public static final String INVALID_PEM_FILE_DIALOG_TITLE =
      "Invalid Private Key (PEM) File";
  static final String BRIEFCASE_WELCOME =
      "Welcome to ODK Briefcase! Here are three things you should know to get started.\n" +
          "1. You must set a Storage Location where Briefcase will store data that it needs to operate. You will not be able to use 		Briefcase until you set this location.\n" +
          "2. We gather anonymous usage data (e.g., operating system, version number) to help our community prioritize fixes and 		features. If you do not want to contribute your data, please disable that setting.\n" +
          "3. ODK is a community-powered project and the community lives at https://forum.opendatakit.org. Stop by for a visit and 		introduce yourself\n\n!" +
          "4. And always remember to have fun!";
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
  public static final String PROXY_TOGGLE = "Use HTTP Proxy";
  public static final String PROXY_SET_ADVICE =
      "If you are behind a proxy, try setting up your proxy details through 'Settings' tab.";

  public static final String PARALLEL_PULLS = "Pull submissions in parallel (experimental)";
  static final String TRACKING_CONSENT_EXPLANATION =
      "Please help the ODK Community of volunteers and our mission to build software that\n" +
          "better meets your needs. We use third-party analytics tools to gather anonymous\n" +
          "information about things like your operating system, versions, and most-used\n" +
          "features of this software. Use of this information will always follow the ODK\n" +
          "Community Privacy Policy.";

  static final String TRACKING_CONSENT = "Gather anonymous usage data";
}