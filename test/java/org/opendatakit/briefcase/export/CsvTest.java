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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Test;
import org.opendatakit.briefcase.matchers.PathMatchers;
import org.opendatakit.briefcase.reused.OverridableBoolean;

public class CsvTest {
  @Test
  public void includes_non_repeat_groups_in_repeat_filenames() throws IOException {
    Model group = new XmlElementTest.ModelBuilder()
        .addGroup("g1")
        .addGroup("g2")
        .addGroup("g3")
        .addRepeatGroup("r")
        .build();

    FormDefinition formDef = new FormDefinition(
        "some_form",
        Files.createTempFile("briefcase_some_form", ".xml"),
        "some_form",
        false,
        group.getParent().getParent().getParent()
    );

    Path exportDir = Files.createTempDirectory("briefcase_export_dir");

    ExportConfiguration conf = new ExportConfiguration(
        Optional.of("some_form.csv"),
        Optional.of(exportDir),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        OverridableBoolean.FALSE,
        OverridableBoolean.TRUE,
        OverridableBoolean.TRUE,
        Optional.of(false)
    );

    Csv repeat = Csv.repeat(formDef, group, conf);
    repeat.prepareOutputFiles();

    assertThat(exportDir.resolve("some_form-g1-g2-g3-r.csv"), PathMatchers.exists());
  }
}