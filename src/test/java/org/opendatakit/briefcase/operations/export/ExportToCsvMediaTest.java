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

package org.opendatakit.briefcase.operations.export;

import static org.opendatakit.briefcase.reused.api.UncheckedFiles.delete;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvMediaTest {
  private ExportToCsvScenario scenario;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("media-files");
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_data_and_media_files() {
    scenario.runExport(true, true);
    scenario.assertSameContent("tt");
    scenario.assertSameMedia("tt");
  }

  @Test
  public void skips_submissions_that_have_missing_media_files() {
    delete(scenario.getSubmissionDir().resolve("1524644764247.jpg"));
    scenario.runExport(true, true);
    scenario.assertSameContent("tt-missing");
    scenario.assertSameMedia("tt-missing");
  }

  @Test
  public void adds_a_numbered_suffix_when_media_files_already_exist_in_the_output_folders() {
    scenario.createOutputFile("media", "1524644764247.jpg");
    scenario.runExport(false, true);
    scenario.assertSameContent("ft");
    scenario.assertSameMedia("ft");
  }

  @Test
  public void adds_a_numbered_suffix_when_media_files_already_exist_in_the_output_folders_even_when_overwriting() {
    scenario.createOutputFile("media", "1524644764247.jpg");
    scenario.runExport(true, true);
    scenario.assertSameContent("tt-dupe");
    scenario.assertSameMedia("tt-dupe");
  }

  @Test
  public void can_export_only_data() {
    scenario.runExport(true, false);
    scenario.assertSameContent("tf");
  }
}
