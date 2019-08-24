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
import static org.opendatakit.briefcase.export.ModelBuilder.group;
import static org.opendatakit.briefcase.export.ModelBuilder.instance;
import static org.opendatakit.briefcase.export.ModelBuilder.repeat;
import static org.opendatakit.briefcase.export.ModelBuilder.text;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    Model model = instance(
        group("g1",
            group("g2",
                group("g3",
                    repeat("r",
                        text("field")
                    )
                )
            )
        )
    ).build();

    FormDefinition formDef = buildFormDef("some_form", model);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form-r.csv"), exists());
  }

  @Test
  public void includes_non_repeat_groups_in_repeat_filenames2() {
    Model model = instance(
        group("g1",
            repeat("r1",
                group("g2",
                    repeat("r2",
                        repeat("r3",
                            text("field")
                        )
                    )
                )
            )
        )
    ).build();

    FormDefinition formDef = buildFormDef("some_form", model);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form-r1.csv"), exists());
    assertThat(exportDir.resolve("some_form-r2.csv"), exists());
    assertThat(exportDir.resolve("some_form-r3.csv"), exists());
  }

  @Test
  public void sanitizes_filenames() {
    Model model = instance(
        group("some-group",
            repeat("re\tpeat",
                text("field")
            )
        )
    ).build();

    FormDefinition formDef = buildFormDef("some.,form", model);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some__form.csv"), exists());
    assertThat(exportDir.resolve("some__form-re peat.csv"), exists());
  }

  @Test
  public void dupe_nested_repeat_group_names_get_a_sequence_number_suffix() {
    Model model = instance(
        repeat("outer-repeat",
            group("outer-group",
                repeat("dupe-repeat",
                    group("inner-group",
                        repeat("dupe-repeat",
                            text("field")
                        )
                    )
                )
            )
        )
    ).build();

    FormDefinition formDef = buildFormDef("some-form", model);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form-outer_repeat.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~1.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~2.csv"), exists());
  }

  @Test
  public void dupe_sibling_repeat_group_names_get_a_sequence_number_suffix() {
    Model model = instance(
        group("group1", repeat("dupe-repeat", text("some-field"))),
        group("group2", repeat("dupe-repeat", text("some-field")))
    ).build();

    FormDefinition formDef = buildFormDef("some-form", model);

    Csv.getCsvs(formDef, conf).forEach(Csv::prepareOutputFiles);

    assertThat(exportDir.resolve("some_form.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~1.csv"), exists());
    assertThat(exportDir.resolve("some_form-dupe_repeat~2.csv"), exists());
  }

  private ExportConfiguration buildConf(Path exportDir) {
    return ExportConfiguration.Builder.empty().setExportDir(exportDir).build();
  }

  private static FormDefinition buildFormDef(String formName, Model group) {
    return new FormDefinition(
        "some_form",
        formName,
        false,
        group, group.getRepeatableFields()
    );
  }
}
