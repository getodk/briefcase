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

package org.opendatakit.briefcase.pull.filesystem;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.IterableMatchers.containsAtLeast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.form.FormMetadata;

public class FormInstallerTest {

  private Path briefcaseDir;
  private Path formsDir;

  @Before
  public void setUp() throws IOException {
    briefcaseDir = createTempDirectory("briefcase_test_");
    formsDir = briefcaseDir.resolve("forms");
    Files.createDirectories(formsDir);

  }

  @Test
  public void installs_the_form_definition_file() throws IOException, URISyntaxException {
    Path sourceFormPath = getPath("basic.xml");

    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormPath);
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(sourceFormMetadata, e -> {});
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );

    assertThat(walk(formsDir).collect(toList()), containsAtLeast(
        formsDir.resolve("basic"),
        formsDir.resolve("basic").resolve("basic.xml")
    ));
  }

  @Test
  public void overwrites_the_form_definition_file_if_needed() throws URISyntaxException, IOException {
    Path sourceFormPath = getPath("basic.xml");

    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormPath);
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(sourceFormMetadata, e -> {});
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );

    assertThat(walk(formsDir).collect(toList()), containsAtLeast(
        formsDir.resolve("basic"),
        formsDir.resolve("basic").resolve("basic.xml")
    ));
  }

  @Test
  public void normalizes_the_filename_of_the_form_definition_file_using_the_form_name() throws URISyntaxException, IOException {
    Path sourceFormPath = getPath("basic-form.xml");

    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormPath);
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(sourceFormMetadata, e -> {});
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );

    List<Path> installedPaths = walk(formsDir).collect(toList());
    assertThat(installedPaths, containsAtLeast(
        formsDir.resolve("basic"),
        formsDir.resolve("basic").resolve("basic.xml")
    ));
    assertThat(installedPaths, not(containsAtLeast(formsDir.resolve("basic").resolve("basic-form.xml"))));
  }

  @Test
  public void installs_any_media_file_if_media_folder_is_found_next_to_the_form_definition_file() throws URISyntaxException, IOException {
    Path sourceFormPath = getPath("Birds.xml");

    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormPath);
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(sourceFormMetadata, e -> {});
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );

    Path expectedFormDir = formsDir.resolve("Birds");
    Path expectedMediaDir = expectedFormDir.resolve("Birds-media");
    assertThat(walk(formsDir).collect(toList()), containsAtLeast(
        expectedFormDir,
        expectedFormDir.resolve("Birds.xml"),
        expectedMediaDir,
        expectedMediaDir.resolve("blackbird.png"),
        expectedMediaDir.resolve("bluethroat.png"),
        expectedMediaDir.resolve("carrioncrow.mp3"),
        expectedMediaDir.resolve("crow.png"),
        expectedMediaDir.resolve("eagle.png"),
        expectedMediaDir.resolve("egret.png"),
        expectedMediaDir.resolve("european-robin.mp3"),
        expectedMediaDir.resolve("goldeneagle.mp3"),
        expectedMediaDir.resolve("goose.png"),
        expectedMediaDir.resolve("gull.png"),
        expectedMediaDir.resolve("hawfinch.png"),
        expectedMediaDir.resolve("jay.png"),
        expectedMediaDir.resolve("kingfisher.3gp"),
        expectedMediaDir.resolve("kingfisher.png"),
        expectedMediaDir.resolve("knot.png"),
        expectedMediaDir.resolve("nuthatch.png"),
        expectedMediaDir.resolve("pelican.png"),
        expectedMediaDir.resolve("pigeon.png"),
        expectedMediaDir.resolve("question.wav"),
        expectedMediaDir.resolve("question-old1.wav"),
        expectedMediaDir.resolve("robin.png"),
        expectedMediaDir.resolve("shag.png"),
        expectedMediaDir.resolve("sparrow.png"),
        expectedMediaDir.resolve("starling.png"),
        expectedMediaDir.resolve("tit.png"),
        expectedMediaDir.resolve("woodpecker.png"),
        expectedMediaDir.resolve("wren.png")
    ));
  }

  @Test
  public void overwrites_any_media_file_if_needed() throws URISyntaxException, IOException {
    Path sourceFormPath = getPath("Birds.xml");

    FormMetadata sourceFormMetadata = FormMetadata.from(sourceFormPath);
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(sourceFormMetadata, e -> {});
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );
    FormInstaller.installForm(
        sourceFormMetadata,
        sourceFormMetadata.withFormFile(sourceFormMetadata.getKey().buildFormFile(briefcaseDir)),
        tracker
    );


    Path expectedFormDir = formsDir.resolve("Birds");
    Path expectedMediaDir = expectedFormDir.resolve("Birds-media");
    assertThat(walk(formsDir).collect(toList()), containsAtLeast(
        expectedFormDir,
        expectedFormDir.resolve("Birds.xml"),
        expectedMediaDir,
        expectedMediaDir.resolve("blackbird.png"),
        expectedMediaDir.resolve("bluethroat.png"),
        expectedMediaDir.resolve("carrioncrow.mp3"),
        expectedMediaDir.resolve("crow.png"),
        expectedMediaDir.resolve("eagle.png"),
        expectedMediaDir.resolve("egret.png"),
        expectedMediaDir.resolve("european-robin.mp3"),
        expectedMediaDir.resolve("goldeneagle.mp3"),
        expectedMediaDir.resolve("goose.png"),
        expectedMediaDir.resolve("gull.png"),
        expectedMediaDir.resolve("hawfinch.png"),
        expectedMediaDir.resolve("jay.png"),
        expectedMediaDir.resolve("kingfisher.3gp"),
        expectedMediaDir.resolve("kingfisher.png"),
        expectedMediaDir.resolve("knot.png"),
        expectedMediaDir.resolve("nuthatch.png"),
        expectedMediaDir.resolve("pelican.png"),
        expectedMediaDir.resolve("pigeon.png"),
        expectedMediaDir.resolve("question.wav"),
        expectedMediaDir.resolve("question-old1.wav"),
        expectedMediaDir.resolve("robin.png"),
        expectedMediaDir.resolve("shag.png"),
        expectedMediaDir.resolve("sparrow.png"),
        expectedMediaDir.resolve("starling.png"),
        expectedMediaDir.resolve("tit.png"),
        expectedMediaDir.resolve("woodpecker.png"),
        expectedMediaDir.resolve("wren.png")
    ));
  }

  private static Path getPath(String fileName) throws URISyntaxException {
    return Paths.get(FormInstallerTest.class.getClassLoader().getResource(fileName).toURI());
  }
}
