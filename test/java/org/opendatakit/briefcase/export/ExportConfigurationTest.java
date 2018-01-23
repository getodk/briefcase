package org.opendatakit.briefcase.export;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.Matchers;
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
    validConfig = ExportConfiguration.empty();
    validConfig.setExportDir(VALID_EXPORT_DIR);
    validConfig.setPemFile(VALID_PEM_FILE);
    validConfig.setStartDate(START_DATE);
    validConfig.setEndDate(END_DATE);
  }

  @Test
  public void it_has_a_factory_that_creates_a_new_instance_from_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(validConfig.asMap());

    ExportConfiguration load = ExportConfiguration.load(prefs);

    assertThat(load, is(validConfig));
  }

  @Test
  public void the_factory_can_load_prefixed_keys_on_saved_preferences() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    prefs.putAll(validConfig.asMap("some_prefix"));

    ExportConfiguration load = ExportConfiguration.load(prefs, "some_prefix");

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
  public void a_configuration_is_empty_when_there_is_no_exportDir() {
    // This feels a little weird because it will be empty after setting
    // the date range or the pem file location
    ExportConfiguration config = ExportConfiguration.empty();
    assertThat(config.isEmpty(), is(true));

    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config.isEmpty(), is(false));
  }

  @Test
  public void an_empty_configuration_is_not_valid() {
    ExportConfiguration config = ExportConfiguration.empty();
    assertThat(config.isValid(), is(false));

    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config.isValid(), is(true));
  }

  @Test
  public void a_configuration_is_not_valid_when_the_start_date_is_after_the_end_date() {
    ExportConfiguration config = ExportConfiguration.empty();
    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config.isValid(), is(true));

    config.setStartDate(END_DATE);
    config.setEndDate(START_DATE);

    assertThat(config.isValid(), is(false));
  }

  @Test
  public void a_configuration_is_not_valid_when_missing_the_end_of_the_date_range() {
    ExportConfiguration config = ExportConfiguration.empty();
    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config.isValid(), is(true));

    config.setStartDate(START_DATE);

    assertThat(config.isValid(), is(false));
  }

  @Test
  public void a_configuration_is_not_valid_when_missing_the_start_of_the_date_range() {
    ExportConfiguration config = ExportConfiguration.empty();
    config.setExportDir(VALID_EXPORT_DIR);
    assertThat(config.isValid(), is(true));

    config.setEndDate(END_DATE);

    assertThat(config.isValid(), is(false));
  }

  @Test
  public void has_an_API_similar_to_Optional_for_its_members() {
    ExportConfiguration emptyConfig = ExportConfiguration.empty();
    assertThat(emptyConfig.mapExportDir(Object::toString), isEmpty());
    assertThat(emptyConfig.mapPemFile(Object::toString), isEmpty());
    assertThat(emptyConfig.mapStartDate(Object::toString), isEmpty());
    assertThat(emptyConfig.mapEndDate(Object::toString), isEmpty());

    emptyConfig.ifExportDirPresent(value -> fail());
    emptyConfig.ifPemFilePresent(value -> fail());
    emptyConfig.ifStartDatePresent(value -> fail());
    emptyConfig.ifEndDatePresent(value -> fail());

    assertThat(validConfig.mapExportDir(Object::toString), isPresent());
    assertThat(validConfig.mapPemFile(Object::toString), isPresent());
    assertThat(validConfig.mapStartDate(Object::toString), isPresent());
    assertThat(validConfig.mapEndDate(Object::toString), isPresent());

    AtomicInteger count = new AtomicInteger(0);
    validConfig.ifExportDirPresent(value -> count.incrementAndGet());
    validConfig.ifPemFilePresent(value -> count.incrementAndGet());
    validConfig.ifStartDatePresent(value -> count.incrementAndGet());
    validConfig.ifEndDatePresent(value -> count.incrementAndGet());
    assertThat(count.get(), is(4));
  }

  @Test
  public void toString_hashCode_and_equals_for_coverage() {
    assertThat(validConfig.toString(), Matchers.containsString("some/dir"));
    assertThat(validConfig.toString(), Matchers.containsString("some_key.pem"));
    assertThat(validConfig.toString(), Matchers.containsString("2018"));
    assertThat(validConfig.toString(), Matchers.containsString("2020"));
    assertThat(validConfig.hashCode(), is(notNullValue()));
    assertThat(validConfig.equals(null), is(false));
    assertThat(validConfig, is(validConfig));
  }
}