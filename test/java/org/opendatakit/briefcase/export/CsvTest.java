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
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.OverridableBoolean;

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
    deleteRecursive(exportDir);
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

    FormDefinition formDef = buildFormDef("some_form", group, 4);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

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

    FormDefinition formDef = buildFormDef("some_form", group, 5);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form-r1.csv"), exists());
    assertThat(exportDir.resolve("some_form-r2.csv"), exists());
    assertThat(exportDir.resolve("some_form-r3.csv"), exists());
  }

  @Test
  public void sanitizes_filenames() {
    Model group = new ModelBuilder()
        .addGroup("data")
        .addGroup("some-group")
        .addRepeatGroup("re\tpeat")
        .build();

    FormDefinition formDef = buildFormDef("some.,form", group, 2);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some__form.csv"), exists());
    assertThat(exportDir.resolve("some__form-re peat.csv"), exists());
  }

  @Test
  public void dupe_repeat_group_names_get_a_sequence_number_suffix() {
    Model group = new ModelBuilder()
        .addGroup("data")
        .addRepeatGroup("outer-repeat")
        .addGroup("outer-group")
        .addRepeatGroup("dupe-repeat")
        .addGroup("inner-group")
        .addRepeatGroup("dupe-repeat")
        .build();

    FormDefinition formDef = buildFormDef("some-form", group, 5);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form-outer_repeat.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~1.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~2.csv"), exists());
  }

  private ExportConfiguration buildConf(Path exportDir) {
    return new ExportConfiguration(
        Optional.empty(),
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

  private static FormDefinition buildFormDef(String formName, Model group, int ancestors) {
    Model root = group;
    for (int i = 0; i < ancestors; i++)
      root = root.getParent();
    return new FormDefinition(
        "some_form",
        Paths.get("/some/random/path/doesnt/matter/"),
        formName,
        false,
        root
    );
  }
}