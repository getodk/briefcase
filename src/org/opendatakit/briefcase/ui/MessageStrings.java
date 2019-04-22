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
  static final String BRIEFCASE_WELCOME = "" +
      "Welcome to ODK Briefcase! Here are three things you should know to get started.\n" +
      "\n" +
      "1. You must set a Storage Location where Briefcase will store data that it needs\n" +
      "    to operate. You will not be able to use Briefcase until you set this location.\n\n" +
      "2. We send usage data (e.g., operating system, version number) and crash logs\n" +
      "    to the core developers to help prioritize features and fixes. If you do not\n" +
      "    want to contribute your usage data or crash logs, please uncheck that setting.\n\n" +
      "3. ODK is a community-powered project and the community lives at\n" +
      "    https://forum.opendatakit.org. Stop by for a visit and introduce yourself!\n" +
      "\n";
  static final String TRACKING_WARNING = "" +
      "We now send usage data (e.g., operating system, version number) and crash logs\n" +
      "to the core developers to help prioritize features and fixes.\n\n" +
      "If you do not want to contribute your usage data or crash logs, please uncheck\n" +
      "that setting in the Settings tab.\n";
}
