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

package org.opendatakit.briefcase.util;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportToCsvTest;

public class FormCacheTest {

  private Path cacheFile;
  private Path briefcaseDir;
  private Path formsDir;

  @Before
  public void setUp() throws IOException {
    briefcaseDir = Files.createTempDirectory("briefcase_test");
    cacheFile = briefcaseDir.resolve("cache.ser");
    formsDir = briefcaseDir.resolve("forms");
    Files.createDirectories(formsDir);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  @Test
  public void updates_itself_scanning_forms_in_the_briefcase_directory() throws IOException {
    FormCache cache = new FormCache(cacheFile, new HashMap<>(), new HashMap<>());
    cache.update(briefcaseDir);

    assertThat(cache.getForms(), is(empty()));

    installForm("nested-repeats");
    installForm("simple-form");

    cache.update(briefcaseDir);

    assertThat(cache.getForms().size(), is(2));
  }

  @Test
  public void removes_items_if_they_are_no_longer_among_forms_in_the_briefcase_directory() throws IOException {
    installForm("simple-form");
    installForm("nested-repeats");

    FormCache cache = new FormCache(cacheFile, new HashMap<>(), new HashMap<>());
    cache.update(briefcaseDir);

    assertThat(cache.getForms().size(), is(2));

    uninstallForm("simple-form");
    cache.update(briefcaseDir);

    assertThat(cache.getForms().size(), is(1));
  }

  private void installForm(final String formName) throws IOException {
    Path formDir = formsDir.resolve(formName);
    Files.createDirectories(formDir);
    Files.copy(
        getPath(formName + ".xml"),
        formDir.resolve(formName + ".xml")
    );
    Files.createDirectories(formDir.resolve("instances"));
    formDir.resolve(formName + ".xml");
  }

  private void uninstallForm(final String formName) {
    deleteRecursive(formsDir.resolve(formName));
  }

  private static Path getPath(String fileName) {
    return maybeGetPath(fileName).orElseThrow(RuntimeException::new);
  }

  private static Optional<Path> maybeGetPath(String fileName) {
    // We're using the export test forms
    return Optional.ofNullable(ExportToCsvTest.class.getClassLoader().getResource("org/opendatakit/briefcase/export/" + fileName))
        .map(url -> Paths.get(toURI(url)));
  }
}