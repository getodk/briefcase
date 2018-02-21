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

import java.nio.file.Path;
import java.security.PrivateKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvEncryptionTest {
  private ExportToCsvScenario scenario;
  private PrivateKey privateKey;
  private Path pemFile;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("encrypted-form");
    pemFile = ExportToCsvScenario.getPath("encrypted-form_key.pem");
    privateKey = ExportConfiguration.readPemFile(pemFile);
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_encrypted_submissions() {
    scenario.runOldExport(privateKey);
    scenario.runNewExport(pemFile);
    scenario.assertSameContent();
  }
}