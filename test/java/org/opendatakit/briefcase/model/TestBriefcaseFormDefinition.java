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
package org.opendatakit.briefcase.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opendatakit.briefcase.util.BadFormDefinition;

/**
 * Test formdef class that emulates BriefcaseFormDefinition's file based behavior
 */
public class TestBriefcaseFormDefinition extends BriefcaseFormDefinition {
  private static final File formDir;
  private static final File formFile;

  static {
    try {
      Path dir = Files.createTempDirectory("briefcase_test");
      Path sourceFile = Paths.get(TestBriefcaseFormDefinition.class.getResource("/basic.xml").toURI());
      Path file = dir.resolve("form.xml");
      Files.copy(sourceFile, file);
      formDir = dir.toFile();
      formFile = file.toFile();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private final int id;

  TestBriefcaseFormDefinition(int id) throws BadFormDefinition {
    super(formDir, formFile);
    this.id = id;
  }

  @Override
  public String getFormName() {
    return "Form #" + id;
  }

  @Override
  public String getFormId() {
    return "" + id;
  }

  @Override
  public String getVersionString() {
    return "1";
  }
}
