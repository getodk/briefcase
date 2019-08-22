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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.empty;
import static org.opendatakit.briefcase.export.ExportConfiguration.Builder.load;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isEmpty;
import static org.opendatakit.briefcase.matchers.ExportConfigurationMatchers.isValid;
import static org.opendatakit.briefcase.reused.TriStateBoolean.FALSE;
import static org.opendatakit.briefcase.reused.TriStateBoolean.TRUE;
import static org.opendatakit.briefcase.reused.TriStateBoolean.UNDETERMINED;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.time.LocalDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration.Builder;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.reused.BriefcaseException;

@SuppressWarnings("checkstyle:MethodName")
public class ExportConfigurationTest {
  private static final ExportConfiguration EMPTY_CONF = empty().build();
  private static final Path BASE_TEMP_DIR = createTempDirectory("briefcase_test");
  private static final Path VALID_EXPORT_DIR = BASE_TEMP_DIR.resolve("export-dir");
  private static final Path VALID_PEM_FILE = BASE_TEMP_DIR.resolve("pkey.pem");
  private static final LocalDate START_DATE = LocalDate.of(2018, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2020, 1, 1);
  private static ExportConfiguration VALID_CONFIG;

  @BeforeClass
  public static void init() throws URISyntaxException {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    createDirectories(VALID_EXPORT_DIR);
    URI sourcePemFileUri = ExportConfigurationTest.class.getClassLoader()
        .getResource("org/opendatakit/briefcase/export/encrypted-form-key.pem")
        .toURI();
    copy(Paths.get(sourcePemFileUri), VALID_PEM_FILE);
    VALID_CONFIG = Builder.empty()
        .setExportDir(VALID_EXPORT_DIR)
        .setPemFile(VALID_PEM_FILE)
        .setStartDate(START_DATE)
        .setEndDate(END_DATE)
        .build();
  }

  @AfterClass
  public static void tearDown() {
    deleteRecursive(BASE_TEMP_DIR);
  }

  @Test
  public void the_builder_rejects_non_existant_export_dirs() {
    Path wrongPath = Paths.get("some/path");
    assertThat(Builder.empty().setExportDir(wrongPath).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_export_dir_paths_to_a_non_directory() {
    Path wrongPath = BASE_TEMP_DIR.resolve("some_file.txt");
    write(wrongPath, "some content");
    assertThat(Builder.empty().setExportDir(wrongPath).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_export_dirs_inside_a_Collect_storage_directory() {
    Path wrongPath = BASE_TEMP_DIR.resolve("odk");
    createDirectories(wrongPath);
    createDirectories(wrongPath.resolve("instances"));
    createDirectories(wrongPath.resolve("forms"));
    assertThat(Builder.empty().setExportDir(wrongPath).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_export_dirs_inside_a_Briefcase_storage_directory() {
    Path wrongPath = BASE_TEMP_DIR.resolve(BriefcasePreferences.BRIEFCASE_DIR);
    createDirectories(wrongPath);
    createDirectories(wrongPath.resolve("forms"));
    assertThat(Builder.empty().setExportDir(wrongPath).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_non_existent_pem_file() {
    Path wrongPath = Paths.get("some/path");
    assertThat(Builder.empty().setPemFile(wrongPath).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_pem_file_paths_to_a_non_file() {
    assertThat(Builder.empty().setPemFile(BASE_TEMP_DIR).build(), isEmpty());
  }

  @Test
  public void the_builder_rejects_pem_files_that_cannot_be_parsed() {
    Path wrongPath = BASE_TEMP_DIR.resolve("some_file.txt");
    write(wrongPath, "some content");
    assertThat(Builder.empty().setPemFile(wrongPath).build(), isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_create_a_conf_with_an_invalid_date_range() {
    Builder.empty()
        .setStartDate(LocalDate.of(2018, 1, 1))
        .setEndDate(LocalDate.of(2017, 1, 1)).build();
  }

  @Test
  public void it_has_a_factory_that_creates_a_new_instance_from_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(VALID_CONFIG.asMap());

    ExportConfiguration load = load(prefs);

    assertThat(load, is(VALID_CONFIG));
  }

  @Test
  public void the_factory_can_load_prefixed_keys_on_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(VALID_CONFIG.asMap("some_prefix"));

    ExportConfiguration load = load(prefs, "some_prefix");

    assertThat(load, is(VALID_CONFIG));
  }

  @Test
  public void a_configuration_is_not_empty_when_any_of_its_properties_are_present() {
    assertThat(EMPTY_CONF, isEmpty());
    assertThat(empty().setExportDir(BASE_TEMP_DIR).build(), not(isEmpty()));
    assertThat(empty().setPemFile(VALID_PEM_FILE).build(), not(isEmpty()));
    assertThat(empty().setStartDate(LocalDate.of(2018, 1, 1)).build(), not(isEmpty()));
    assertThat(empty().setEndDate(LocalDate.of(2018, 1, 1)).build(), not(isEmpty()));
    assertThat(empty().setPullBefore(true).build(), not(isEmpty()));
    assertThat(empty().setOverwriteFiles(true).build(), not(isEmpty()));
    assertThat(empty().setExportMedia(true).build(), not(isEmpty()));
    assertThat(empty().setSplitSelectMultiples(true).build(), not(isEmpty()));
    assertThat(empty().setIncludeGeoJsonExport(true).build(), not(isEmpty()));
    assertThat(empty().setRemoveGroupNames(true).build(), not(isEmpty()));
  }

  @Test
  public void a_configuration_is_empty_if_overrideable_boolean_params_contain_INHERIT() {
    assertThat(empty().overridePullBefore(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overridePullBefore(TRUE).build(), not(isEmpty()));
    assertThat(empty().overridePullBefore(FALSE).build(), not(isEmpty()));
    assertThat(empty().overrideOverwriteFiles(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overrideOverwriteFiles(TRUE).build(), not(isEmpty()));
    assertThat(empty().overrideOverwriteFiles(FALSE).build(), not(isEmpty()));
    assertThat(empty().overrideExportMedia(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overrideExportMedia(TRUE).build(), not(isEmpty()));
    assertThat(empty().overrideExportMedia(FALSE).build(), not(isEmpty()));
    assertThat(empty().overrideSplitSelectMultiples(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overrideSplitSelectMultiples(TRUE).build(), not(isEmpty()));
    assertThat(empty().overrideSplitSelectMultiples(FALSE).build(), not(isEmpty()));
    assertThat(empty().overrideIncludeGeoJsonExport(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overrideIncludeGeoJsonExport(TRUE).build(), not(isEmpty()));
    assertThat(empty().overrideIncludeGeoJsonExport(FALSE).build(), not(isEmpty()));
    assertThat(empty().overrideRemoveGroupNames(UNDETERMINED).build(), isEmpty());
    assertThat(empty().overrideRemoveGroupNames(TRUE).build(), not(isEmpty()));
    assertThat(empty().overrideRemoveGroupNames(FALSE).build(), not(isEmpty()));
  }

  @Test
  public void an_empty_configuration_is_not_valid() {
    assertThat(EMPTY_CONF, not(isValid()));
  }

  @Test
  public void pull_before_export_defaults_to_false_on_empty_confs() {
    assertThat(EMPTY_CONF.resolvePullBefore(), is(false));
  }

  @Test
  public void overwrite_files_defaults_to_false_on_empty_confs() {
    assertThat(EMPTY_CONF.resolveOverwriteExistingFiles(), is(false));
  }

  @Test
  public void export_media_defaults_to_true_on_empty_confs() {
    assertThat(EMPTY_CONF.resolveExportMedia(), is(true));
  }

  @Test
  public void split_select_multiples_defaults_to_false_on_empty_confs() {
    assertThat(EMPTY_CONF.resolveSplitSelectMultiples(), is(false));
  }

  @Test
  public void include_geojson_export_defaults_to_false_on_empty_confs() {
    assertThat(EMPTY_CONF.resolveIncludeGeoJsonExport(), is(false));
  }

  @Test
  public void remove_group_names_defaults_to_false_on_empty_confs() {
    assertThat(EMPTY_CONF.resolveRemoveGroupNames(), is(false));
  }

  @Test
  public void builds_the_path_and_filenames_of_output_audit_files() {
    assertThat(VALID_CONFIG.getAuditPath("some-form").getFileName().toString(), is("some-form - audit.csv"));
    assertThat(VALID_CONFIG.getAuditPath("some-form").getFileName().toString(), is("some-form - audit.csv"));
  }

  @Test(expected = BriefcaseException.class)
  public void an_empty_conf_will_throw_when_asked_for_the_output_audit_file() {
    EMPTY_CONF.getAuditPath("some-form");
  }

  @Test
  public void builds_the_sanitized_export_filename_base_with_the_form_name_and_an_optional_filename_param() {
    assertThat(buildConf("some_filename.csv").getFilenameBase("Some Form"), is("some_filename"));
    assertThat(buildConf(null).getFilenameBase("Some Form"), is("Some Form"));
    assertThat(buildConf("some,.-filename.csv").getFilenameBase("Some Form"), is("some___filename"));
    assertThat(buildConf(null).getFilenameBase("Some ,.- Form"), is("Some ___ Form"));
  }

  private ExportConfiguration buildConf(String exportFileName) {
    return Builder.empty().setExportFilename(exportFileName).build();
  }
}