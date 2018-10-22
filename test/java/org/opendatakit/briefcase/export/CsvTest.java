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

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.reused.UncheckedFiles;

public class CsvTest {
  private Path exportDir;
  private ExportConfiguration conf;

  @Before
  public void setup() throws IOException {
    exportDir = Files.createTempDirectory("briefcase_export_dir");
    conf = buildConf(exportDir);
  }

  @After
  public void tearDown() {
    UncheckedFiles.deleteRecursive(exportDir);
  }

  @Test
  public void includes_non_repeat_groups_in_repeat_filenames() {
    Model group = new ModelBuilder()
        .addGroup("data")
        .addGroup("g1")
        .addGroup("g2")
        .addGroup("g3")
        .addRepeatGroup("r")
        .build();

    FormDefinition formDef = buildFormDef(group, 4);

    Csv.repeat(formDef, group, conf).prepareOutputFiles();

    assertThat(exportDir.resolve("some_form-r.csv"), exists());
  }

  @Test
  public void includes_non_repeat_groups_in_repeat_filenames2() {
    Model group = new ModelBuilder()
        .addGroup("data")
        .addGroup("g1")
        .addRepeatGroup("r1")
        .addGroup("g2")
        .addRepeatGroup("r2")
        .addRepeatGroup("r3")
        .build();

    FormDefinition formDef = buildFormDef(group, 5);

    Csv.repeat(formDef, group, conf).prepareOutputFiles();
    Csv.repeat(formDef, group.getParent(), conf).prepareOutputFiles();
    Csv.repeat(formDef, group.getParent().getParent().getParent(), conf).prepareOutputFiles();

    assertThat(exportDir.resolve("some_form-r1.csv"), exists());
    assertThat(exportDir.resolve("some_form-r2.csv"), exists());
    assertThat(exportDir.resolve("some_form-r3.csv"), exists());
  }

  private ExportConfiguration buildConf(Path exportDir) {
    return new ExportConfiguration(
        Optional.of("some_form.csv"),
        Optional.of(exportDir),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        OverridableBoolean.FALSE,
        OverridableBoolean.TRUE,
        OverridableBoolean.TRUE,
        OverridableBoolean.FALSE
    );
  }

  private static FormDefinition buildFormDef(Model group, int ancestors) {
    Model root = group;
    for (int i = 0; i < ancestors; i++)
      root = root.getParent();
    return new FormDefinition(
        "some_form",
        Paths.get("/some/random/path/doesnt/matter/"),
        "some_form",
        false,
        root
    );
  }
}