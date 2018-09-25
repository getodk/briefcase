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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.opendatakit.briefcase.export.ExportConfiguration.empty;
import static org.opendatakit.briefcase.export.ExportConfiguration.load;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isEmpty;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isValid;
import static org.opendatakit.briefcase.reused.TriStateBoolean.FALSE;
import static org.opendatakit.briefcase.reused.TriStateBoolean.TRUE;
import static org.opendatakit.briefcase.reused.TriStateBoolean.UNDETERMINED;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.reused.OverridableBoolean;

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
    ExportConfiguration copiedConf = validConfig.copy();
    assertThat(copiedConf, not(sameInstance(validConfig)));
    assertThat(copiedConf, equalTo(validConfig));
  }

  @Test
  public void a_configuration_is_not_empty_when_any_of_its_properties_is_present() {
    assertThat(empty(), isEmpty());
    assertThat(empty().setExportDir(Paths.get("/some/path")), not(isEmpty()));
    assertThat(empty().setPemFile(Paths.get("/some/file.pem")), not(isEmpty()));
    assertThat(empty().setStartDate(LocalDate.of(2018, 1, 1)), not(isEmpty()));
    assertThat(empty().setEndDate(LocalDate.of(2018, 1, 1)), not(isEmpty()));
    ExportConfiguration emptyOne = empty();
    emptyOne.pullBefore.set(true);
    assertThat(emptyOne, not(isEmpty()));
    ExportConfiguration emptyTwo = empty();
    emptyTwo.pullBefore.overrideWith(TRUE);
    assertThat(emptyTwo, not(isEmpty()));
  }

  @Test
  public void a_configuration_is_empty_if_pull_before_override_contains_INHERIT() {
    ExportConfiguration emptyOne = empty();
    emptyOne.pullBefore.overrideWith(UNDETERMINED);
    assertThat(emptyOne, isEmpty());
    ExportConfiguration emptyTwo = empty();
    emptyTwo.pullBefore.overrideWith(TRUE);
    assertThat(emptyTwo, not(isEmpty()));
    ExportConfiguration emptyThree = empty();
    emptyThree.pullBefore.overrideWith(FALSE);
    assertThat(emptyThree, not(isEmpty()));
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
  public void defaults_to_true_when_the_export_media_param_is_empty() {
    assertThat(empty().resolveExportMedia(), is(true));
  }

  @Test
  public void defaults_to_false_when_pull_before_is_empty() {
    assertThat(empty().resolvePullBefore(), is(false));
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

  @Test
  public void ensures_the_export_filename_has_csv_extension() {
    assertThat(buildConf("some_filename").getExportFileName(), OptionalMatchers.isPresentAnd(is("some_filename.csv")));
    assertThat(buildConf("some_filename.csv").getExportFileName(), OptionalMatchers.isPresentAnd(is("some_filename.csv")));
    assertThat(buildConf("some_filename.CSV").getExportFileName(), OptionalMatchers.isPresentAnd(is("some_filename.CSV")));
    assertThat(buildConf("some_filename.cSv").getExportFileName(), OptionalMatchers.isPresentAnd(is("some_filename.cSv")));
  }

  public ExportConfiguration buildConf(String exportFileName) {
    return new ExportConfiguration(
        Optional.of(exportFileName),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        OverridableBoolean.empty(),
        OverridableBoolean.empty(),
        OverridableBoolean.empty(),
        OverridableBoolean.FALSE
    );
  }


}