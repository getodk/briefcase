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
package org.opendatakit.briefcase.reused.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.isInstanceDir;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.write;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("checkstyle:MethodName")
public class UncheckedFilesIsInstanceDirTest {
  private static Path tempDir;

  @Before
  public void setUp() {
    tempDir = createTempDirectory("briefcase");
  }

  @After
  public void tearDown() {
    deleteRecursive(tempDir);
  }

  @Test
  public void a_non_existing_path_is_not_an_instance_directory() {
    assertThat(isInstanceDir(tempDir.resolve("uuid12345678-1234-1234-1234-123456789012")), is(false));
  }

  @Test
  public void a_file_is_not_an_instance_directory() {
    Path someFile = tempDir.resolve("some_file.txt");
    write(someFile, Stream.empty());
    assertThat(isInstanceDir(someFile), is(false));
  }

  @Test
  public void an_instance_dir_must_match_a_certain_name_and_contain_a_submissions_dot_xml_file() {
    Path correctDir = createDirectories(tempDir.resolve("uuid12345678-1234-1234-1234-123456789012"));
    write(correctDir.resolve("submission.xml"), Stream.empty());
    assertThat(isInstanceDir(createDirectories(correctDir)), is(true));
  }

  @Test
  public void an_arbitrarily_named_directory_with_a_submission_dot_xml_file_is_also_an_instance_directory() {
    Path correctDir = createDirectories(tempDir.resolve("some_directory"));
    write(correctDir.resolve("submission.xml"), Stream.empty());
    assertThat(isInstanceDir(createDirectories(correctDir)), is(true));
  }

  @Test
  public void a_linux_or_mac_hidden_directory_is_not_an_instance_directory_even_if_it_has_a_submission_dot_xml_file() {
    Path correctDir = createDirectories(tempDir.resolve(".some_hidden_directory"));
    write(correctDir.resolve("submission.xml"), Stream.empty());
    assertThat(isInstanceDir(createDirectories(correctDir)), is(false));
  }
}
