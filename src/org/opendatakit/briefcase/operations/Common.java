/*
 * Copyright (C) 2018 Nafundi
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
package org.opendatakit.briefcase.operations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.common.cli.Param;

public class Common {
  public static final Param<String> STORAGE_DIR = Param.arg("sd", "storage_directory", "Briefcase storage directory");
  public static final Param<String> FORM_ID = Param.arg("id", "form_id", "Form ID");
  public static final Param<String> ODK_USERNAME = Param.arg("u", "odk_username", "ODK Username");
  public static final Param<String> ODK_PASSWORD = Param.arg("p", "odk_password", "ODK Password");
  public static final Param<String> AGGREGATE_SERVER = Param.arg("url", "aggregate_url", "Aggregate server URL");

  static Path getOrCreateBriefcaseDir(String storageDir) {
    Path briefcaseDir = BriefcasePreferences.buildBriefcaseDir(Paths.get(storageDir));
    if (!Files.exists(briefcaseDir)) {
      System.err.println("The directory " + briefcaseDir.toString() + " doesn't exist. Creating it");
      UncheckedFiles.createBriefcaseDir(briefcaseDir);
    }
    return briefcaseDir;
  }
}
