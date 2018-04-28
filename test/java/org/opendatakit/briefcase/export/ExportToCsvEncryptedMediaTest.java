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

package org.opendatakit.briefcase.export;

import static org.opendatakit.briefcase.reused.UncheckedFiles.delete;

import java.nio.file.Path;
import java.security.PrivateKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvEncryptedMediaTest {
  private ExportToCsvScenario scenario;
  private PrivateKey privateKey;
  private Path pemFile;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("encrypted-form-media");
    pemFile = ExportToCsvScenario.getPath("encrypted-form-media_key.pem");
    privateKey = ExportConfiguration.readPemFile(pemFile);
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_encrypted_media_files() {
    scenario.runOldExport(privateKey);
    scenario.runNewExport(pemFile);
    scenario.assertSameContent();
    scenario.assertSameMedia();
  }

  @Test
  public void skips_submissions_that_are_missing_their_media_files() {
    delete(scenario.getSubmissionDir().resolve("1524644764247.jpg.enc"));
    scenario.runOldExport(privateKey);
    scenario.runNewExport(pemFile);
    scenario.assertSameContent();
    scenario.assertNoOutputMediaDir();
  }

}