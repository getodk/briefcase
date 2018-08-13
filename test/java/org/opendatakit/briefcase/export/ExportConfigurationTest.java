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

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.opendatakit.briefcase.export.ExportConfiguration.empty;
import static org.opendatakit.briefcase.export.ExportConfiguration.load;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isEmpty;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isValid;
import static org.opendatakit.briefcase.ui.export.components.CustomConfBoolean.Value.INHERIT;
import static org.opendatakit.briefcase.ui.export.components.CustomConfBoolean.Value.NO;
import static org.opendatakit.briefcase.ui.export.components.CustomConfBoolean.Value.YES;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.InMemoryPreferences;

@SuppressWarnings("checkstyle:MethodName")
public class ExportConfigurationTest {
  private ExportConfiguration validConfig;
  private static final Path VALID_EXPORT_DIR;
  private static final Path VALID_PEM_FILE;
  private static final LocalDate START_DATE = LocalDate.of(2018, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2020, 1, 1);

  static {
    try {
      VALID_EXPORT_DIR = Paths.get(createTempDirectory("briefcase_test").toUri()).resolve("some/dir");
      VALID_PEM_FILE = VALID_EXPORT_DIR.resolve("some_key.pem");
      Files.createDirectories(VALID_EXPORT_DIR);
      write(VALID_PEM_FILE, "some content".getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    validConfig = empty();
    validConfig.setExportDir(VALID_EXPORT_DIR);
    validConfig.setPemFile(VALID_PEM_FILE);
    validConfig.setStartDate(START_DATE);
    validConfig.setEndDate(END_DATE);
  }

  @Test
  public void it_has_a_factory_that_creates_a_new_instance_from_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(validConfig.asMap());

    ExportConfiguration load = load(prefs);

    assertThat(load, is(validConfig));
  }

  @Test
  public void the_factory_can_load_prefixed_keys_on_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(validConfig.asMap("some_prefix"));

    ExportConfiguration load = load(prefs, "some_prefix");

    assertThat(load, is(validConfig));
  }

  @Test
  public void knows_how_to_clone_an_instance() {
    ExportConfiguration clonedConfig = validConfig.copy();
    assertThat(clonedConfig == validConfig, is(false));
    assertThat(clonedConfig, is(validConfig));
    // Pump the coverage up making the test go through the getters as well
    assertThat(clonedConfig.getExportDir(), is(validConfig.getExportDir()));
    assertThat(clonedConfig.getPemFile(), is(validConfig.getPemFile()));
    assertThat(clonedConfig.getStartDate(), is(validConfig.getStartDate()));
    assertThat(clonedConfig.getEndDate(), is(validConfig.getEndDate()));
  }

  @Test
  public void a_configuration_is_not_empty_when_any_of_its_properties_is_present() {
    assertThat(empty(), isEmpty());
    assertThat(empty().setExportDir(Paths.get("/some/path")), not(isEmpty()));
    assertThat(empty().setPemFile(Paths.get("/some/file.pem")), not(isEmpty()));
    assertThat(empty().setStartDate(LocalDate.of(2018, 1, 1)), not(isEmpty()));
    assertThat(empty().setEndDate(LocalDate.of(2018, 1, 1)), not(isEmpty()));
    assertThat(empty().setPullBefore(true), not(isEmpty()));
    assertThat(empty().setPullBeforeOverride(YES), not(isEmpty()));
  }

  @Test
  public void a_configuration_is_empty_if_pull_before_override_contains_INHERIT() {
    assertThat(empty().setPullBeforeOverride(INHERIT), isEmpty());
    assertThat(empty().setPullBeforeOverride(YES), not(isEmpty()));
    assertThat(empty().setPullBeforeOverride(NO), not(isEmpty()));
  }

  @Test
  public void an_empty_configuration_is_not_valid() {
    assertThat(empty(), not(isValid()));
    assertThat(empty().setExportDir(VALID_EXPORT_DIR), isValid());
  }

  @Test
  public void a_configuration_is_not_valid_when_the_start_date_is_after_the_end_date() {
    ExportConfiguration config = empty().setExportDir(VALID_EXPORT_DIR);
    assertThat(config, isValid());

    config.setStartDate(END_DATE).setEndDate(START_DATE);

    assertThat(config, not(isValid()));
  }

  @Test
  public void a_configuration_is_not_valid_when_missing_the_end_of_the_date_range() {
    ExportConfiguration config = empty();
    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config, isValid());

    config.setStartDate(START_DATE);

    assertThat(config, not(isValid()));
  }

  @Test
  public void a_configuration_is_not_valid_when_missing_the_start_of_the_date_range() {
    ExportConfiguration config = empty();
    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config, isValid());

    config.setEndDate(END_DATE);

    assertThat(config, not(isValid()));
  }

  @Test
  public void resolves_if_we_need_to_pull_depending_on_a_pair_or_fields() {
    assertThat(empty().resolvePullBefore(), is(false));
    assertThat(empty().setPullBefore(true).resolvePullBefore(), is(true));
    assertThat(empty().setPullBefore(false).resolvePullBefore(), is(false));
    assertThat(empty().setPullBefore(true).setPullBeforeOverride(INHERIT).resolvePullBefore(), is(true));
    assertThat(empty().setPullBefore(true).setPullBeforeOverride(YES).resolvePullBefore(), is(true));
    assertThat(empty().setPullBefore(true).setPullBeforeOverride(NO).resolvePullBefore(), is(false));
    assertThat(empty().setPullBefore(false).setPullBeforeOverride(INHERIT).resolvePullBefore(), is(false));
    assertThat(empty().setPullBefore(false).setPullBeforeOverride(YES).resolvePullBefore(), is(true));
    assertThat(empty().setPullBefore(false).setPullBeforeOverride(NO).resolvePullBefore(), is(false));
  }

  @Test
  public void resolves_if_we_need_to_export_media_files_depending_on_a_pair_or_fields() {
    assertThat(empty().resolveExportMedia(), is(true));
    assertThat(empty().setExportMedia(true).resolveExportMedia(), is(true));
    assertThat(empty().setExportMedia(false).resolveExportMedia(), is(false));
    assertThat(empty().setExportMedia(true).setExportMediaOverride(INHERIT).resolveExportMedia(), is(true));
    assertThat(empty().setExportMedia(true).setExportMediaOverride(YES).resolveExportMedia(), is(true));
    assertThat(empty().setExportMedia(true).setExportMediaOverride(NO).resolveExportMedia(), is(false));
    assertThat(empty().setExportMedia(false).setExportMediaOverride(INHERIT).resolveExportMedia(), is(false));
    assertThat(empty().setExportMedia(false).setExportMediaOverride(YES).resolveExportMedia(), is(true));
    assertThat(empty().setExportMedia(false).setExportMediaOverride(NO).resolveExportMedia(), is(false));
  }

  @Test
  public void has_an_API_similar_to_Optional_for_some_of_its_members() {
    ExportConfiguration emptyConfig = empty();
    assertThat(emptyConfig.mapExportDir(Object::toString), OptionalMatchers.isEmpty());
    assertThat(emptyConfig.mapPemFile(Object::toString), OptionalMatchers.isEmpty());
    assertThat(emptyConfig.mapStartDate(Object::toString), OptionalMatchers.isEmpty());
    assertThat(emptyConfig.mapEndDate(Object::toString), OptionalMatchers.isEmpty());

    emptyConfig.ifExportDirPresent(value -> fail());
    emptyConfig.ifPemFilePresent(value -> fail());
    emptyConfig.ifStartDatePresent(value -> fail());
    emptyConfig.ifEndDatePresent(value -> fail());

    assertThat(validConfig.mapExportDir(Object::toString), OptionalMatchers.isPresent());
    assertThat(validConfig.mapPemFile(Object::toString), OptionalMatchers.isPresent());
    assertThat(validConfig.mapStartDate(Object::toString), OptionalMatchers.isPresent());
    assertThat(validConfig.mapEndDate(Object::toString), OptionalMatchers.isPresent());

    AtomicInteger count = new AtomicInteger(0);
    validConfig.ifExportDirPresent(value -> count.incrementAndGet());
    validConfig.ifPemFilePresent(value -> count.incrementAndGet());
    validConfig.ifStartDatePresent(value -> count.incrementAndGet());
    validConfig.ifEndDatePresent(value -> count.incrementAndGet());
    assertThat(count.get(), is(4));
  }

  @Test
  public void toString_hashCode_and_equals_for_coverage() {
    assertThat(validConfig.toString(), containsString("some" + File.separator + "dir"));
    assertThat(validConfig.toString(), containsString("some_key.pem"));
    assertThat(validConfig.toString(), containsString("2018"));
    assertThat(validConfig.toString(), containsString("2020"));
    assertThat(validConfig.hashCode(), is(notNullValue()));
    assertThat(validConfig.equals(null), is(false));
    assertThat(validConfig, is(validConfig));
  }
}