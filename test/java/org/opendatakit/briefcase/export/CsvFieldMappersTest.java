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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;
import static org.junit.Assert.assertThat;
import static org.kxml2.kdom.Node.ELEMENT;
import static org.kxml2.kdom.Node.TEXT;
import static org.opendatakit.briefcase.export.CsvFieldMappersTest.Scenario.nonGroup;
import static org.opendatakit.briefcase.export.CsvFieldMappersTest.Scenario.nonRepeatGroup;
import static org.opendatakit.briefcase.export.CsvFieldMappersTest.Scenario.repeatGroup;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.IntStream;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.instance.TreeElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.UncheckedFiles;

public class CsvFieldMappersTest {
  private TimeZone backupTimeZone;
  private Locale backupLocale;
  private Scenario scenario;

  @Before
  public void setUp() {
    backupTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    backupLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    scenario = null;
  }

  @After
  public void tearDown() {
    TimeZone.setDefault(backupTimeZone);
    Locale.setDefault(backupLocale);
    scenario.getPaths().forEach(UncheckedFiles::deleteRecursive);
  }

  @Test
  public void text_value() {
    scenario = nonGroup(DataType.TEXT);

    List<Pair<String, String>> output = scenario.mapSimpleValue("some, text");

    assertThat(output, hasSize(1));
    assertThat(output.get(0).getRight(), is("some, text"));
  }

  @Test
  public void date_value() {
    scenario = nonGroup(DataType.DATE);

    List<Pair<String, String>> output = scenario.mapSimpleValue("2018-01-01");

    assertThat(output, hasSize(1));
    assertThat(output.get(0).getRight(), is("Jan 1, 2018"));
  }

  @Test
  public void time_value() {
    scenario = nonGroup(DataType.TIME);

    List<Pair<String, String>> output = scenario.mapSimpleValue("17:30:15.123Z");

    assertThat(output, hasSize(1));
    assertThat(output.get(0).getRight(), is("5:30:15 PM"));
  }

  @Test
  public void date_time_value() {
    scenario = nonGroup(DataType.DATE_TIME);

    List<Pair<String, String>> output = scenario.mapSimpleValue("2018-01-01T17:30:15.123Z");

    assertThat(output, hasSize(1));
    assertThat(output.get(0).getRight(), is("Jan 1, 2018 5:30:15 PM"));
  }

  @Test
  public void geopoint_value() {
    scenario = nonGroup(DataType.GEOPOINT);

    List<Pair<String, String>> output = scenario.mapSimpleValue("1 2 3 4");

    assertThat(output, hasSize(4));
    assertThat(output.get(0).getRight(), is("1"));
    assertThat(output.get(1).getRight(), is("2"));
    assertThat(output.get(2).getRight(), is("3"));
    assertThat(output.get(3).getRight(), is("4"));
  }

  @Test
  public void partial_geopoint_value() {
    scenario = nonGroup(DataType.GEOPOINT);

    List<Pair<String, String>> output = scenario.mapSimpleValue("1 2");

    assertThat(output, hasSize(4));
    assertThat(output.get(0).getRight(), is("1"));
    assertThat(output.get(1).getRight(), is("2"));
    assertThat(output.get(2).getRight(), nullValue());
    assertThat(output.get(3).getRight(), nullValue());
  }

  @Test
  public void binary_value_given_user_does_not_want_to_export_media_files() {
    scenario = nonGroup(DataType.BINARY);
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), UUID.randomUUID().toString());

    List<Pair<String, String>> outputWithoutMedia = scenario.mapSimpleValue("some_file.bin", false);

    assertThat(outputWithoutMedia, hasSize(1));
    // For some reason, we don't prefix the media folder in this case
    assertThat(outputWithoutMedia.get(0).getRight(), is("some_file.bin"));
  }

  @Test
  public void binary_value_given_user_wants_to_export_media_files() {
    scenario = nonGroup(DataType.BINARY);
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), UUID.randomUUID().toString());

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file.bin"));
  }

  @Test
  public void binary_value_given_source_file_does_not_exist() {
    scenario = nonGroup(DataType.BINARY);

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file.bin"));
  }

  /**
   * Binary files have filesystem side-effects. They copy media files
   * from the working directory to the output media directory.
   */
  @Test
  public void binary_value_given_output_file_does_not_exist() {
    scenario = nonGroup(DataType.BINARY);
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), UUID.randomUUID().toString());

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file.bin"));
    assertThat(scenario.getOutputMediaDir().resolve("some_file.bin"), exists());
    assertThat(list(scenario.getOutputMediaDir()).collect(toList()), hasSize(1));
  }

  /**
   * When the destination file exists and it contains exactly the same
   * contents, no file is copied. The absence of this side-effect is hard to test,
   * but, at least we can test that the expected destination file is there.
   */
  @Test
  public void binary_value_given_exact_same_output_file_exist() {
    scenario = nonGroup(DataType.BINARY);
    String fileContents = UUID.randomUUID().toString();
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), fileContents);
    UncheckedFiles.write(scenario.getOutputMediaDir().resolve("some_file.bin"), fileContents);

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file.bin"));
    assertThat(scenario.getOutputMediaDir().resolve("some_file.bin"), exists());
    assertThat(list(scenario.getOutputMediaDir()).collect(toList()), hasSize(1));
  }

  /**
   * When the destination file exists and it contains a different output
   * (it's not clear when could this happen), the source media file is copied
   * with a sequence number suffixes to its filename.
   */
  @Test
  public void binary_value_given_different_output_file_exist() {
    scenario = nonGroup(DataType.BINARY);
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), UUID.randomUUID().toString());
    UncheckedFiles.write(scenario.getOutputMediaDir().resolve("some_file.bin"), UUID.randomUUID().toString());

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file-2.bin"));
    assertThat(scenario.getOutputMediaDir().resolve("some_file.bin"), exists());
    assertThat(scenario.getOutputMediaDir().resolve("some_file-2.bin"), exists());
    assertThat(list(scenario.getOutputMediaDir()).collect(toList()), hasSize(2));
  }

  /**
   * The sequence number suffix on exported media files can go beyond 2.
   */
  @Test
  public void binary_value_given_different_output_files_exist() {
    scenario = nonGroup(DataType.BINARY);
    UncheckedFiles.write(scenario.getWorkDir().resolve("some_file.bin"), UUID.randomUUID().toString());
    UncheckedFiles.write(scenario.getOutputMediaDir().resolve("some_file.bin"), UUID.randomUUID().toString());
    UncheckedFiles.write(scenario.getOutputMediaDir().resolve("some_file-2.bin"), UUID.randomUUID().toString());

    List<Pair<String, String>> outputWithMedia = scenario.mapSimpleValue("some_file.bin", true);

    assertThat(outputWithMedia, hasSize(1));
    assertThat(outputWithMedia.get(0).getRight(), is("media/some_file-3.bin"));
    assertThat(scenario.getOutputMediaDir().resolve("some_file.bin"), exists());
    assertThat(scenario.getOutputMediaDir().resolve("some_file-2.bin"), exists());
    assertThat(scenario.getOutputMediaDir().resolve("some_file-3.bin"), exists());
    assertThat(list(scenario.getOutputMediaDir()).collect(toList()), hasSize(3));
  }

  @Test
  public void repeat_group_value() {
    scenario = repeatGroup("instance_1", DataType.TEXT, 1);

    List<Pair<String, String>> output = scenario.mapGroupValue("some value");

    assertThat(output, hasSize(1));
    assertThat(output.get(0).getRight(), is("instance_1/data-group"));
  }

  @Test
  public void non_repeat_group_value() {
    scenario = nonRepeatGroup(DataType.TEXT, 3);

    List<Pair<String, String>> output = scenario.mapGroupValue("some value 1", "some value 2", "some value 3");

    assertThat(output, hasSize(3));
    assertThat(output.get(0).getRight(), is("some value 1"));
    assertThat(output.get(1).getRight(), is("some value 2"));
    assertThat(output.get(2).getRight(), is("some value 3"));
  }


  static class Scenario {
    private static int instanceIdSeq = 1;
    private final Path workDir = createTempDirectory("briefcase_test_workdir");
    private final Path outputMediaDir = UncheckedFiles.createDirectories(createTempDirectory("briefcase_test_media").resolve("media"));
    private final String instanceId;
    private final String instanceName;
    private final String fieldName;
    private final Model fieldModel;

    Scenario(String instanceId, String instanceName, String fieldName, Model fieldModel) {
      this.instanceId = instanceId;
      this.instanceName = instanceName;
      this.fieldName = fieldName;
      this.fieldModel = fieldModel;
    }

    static Scenario nonGroup(DataType dataType) {
      Model fieldModel = createField(dataType);
      return new Scenario("instance_" + instanceIdSeq++, "data", "field", fieldModel);
    }

    static Model createField(DataType dataType) {
      TreeElement fieldTreeElement = new TreeElement("field", DEFAULT_MULTIPLICITY);
      fieldTreeElement.setDataType(dataType.value);

      TreeElement instanceTreeElement = new TreeElement("data", DEFAULT_MULTIPLICITY);
      instanceTreeElement.setDataType(DataType.NULL.value);

      TreeElement rootTreeElement = new TreeElement("/", DEFAULT_MULTIPLICITY);
      rootTreeElement.setDataType(DataType.NULL.value);

      rootTreeElement.addChild(instanceTreeElement);
      instanceTreeElement.addChild(fieldTreeElement);

      instanceTreeElement.setParent(rootTreeElement);
      fieldTreeElement.setParent(instanceTreeElement);

      return new Model(fieldTreeElement, Collections.emptyMap());
    }

    private static Scenario group(String instanceId, DataType dataType, int fieldCount, boolean repeatable) {
      List<TreeElement> groupFieldTreeElements = IntStream.range(0, fieldCount).boxed().map(i -> {
        TreeElement field = new TreeElement("field_" + (i + 1), DEFAULT_MULTIPLICITY);
        field.setDataType(dataType.value);
        return field;
      }).collect(toList());

      TreeElement groupTreeElement = new TreeElement("group", DEFAULT_MULTIPLICITY);
      groupTreeElement.setDataType(DataType.NULL.value);
      groupTreeElement.setRepeatable(repeatable);

      TreeElement instanceTreeElement = new TreeElement("data", DEFAULT_MULTIPLICITY);
      instanceTreeElement.setDataType(DataType.NULL.value);

      TreeElement rootTreeElement = new TreeElement("/", DEFAULT_MULTIPLICITY);
      rootTreeElement.setDataType(DataType.NULL.value);

      rootTreeElement.addChild(instanceTreeElement);
      instanceTreeElement.addChild(groupTreeElement);
      groupFieldTreeElements.forEach(groupTreeElement::addChild);

      groupFieldTreeElements.forEach(field -> field.setParent(groupTreeElement));
      groupTreeElement.setParent(instanceTreeElement);
      instanceTreeElement.setParent(rootTreeElement);

      return new Scenario(instanceId, "data", "group", new Model(groupTreeElement, Collections.emptyMap()));
    }

    static Scenario repeatGroup(String instanceId, DataType dataType, int fieldCount) {
      return group(instanceId, dataType, fieldCount, true);
    }

    static Scenario nonRepeatGroup(DataType dataType, int fieldCount) {
      return group("instance_" + instanceIdSeq++, dataType, fieldCount, false);
    }

    private XmlElement buildSimpleValueSubmission(String value) {
      Document xmlDoc = new Document();

      Element xmlRoot = new Element();

      Element xmlInstance = new Element();
      xmlInstance.setName(instanceName);

      Element xmlField = new Element();
      xmlField.setName(fieldName);

      xmlDoc.addChild(ELEMENT, xmlRoot);
      xmlRoot.addChild(ELEMENT, xmlInstance);
      xmlInstance.addChild(ELEMENT, xmlField);
      xmlField.addChild(TEXT, value);

      return new XmlElement(xmlField);
    }

    private XmlElement buildGroupValueSubmission(String... values) {
      Document xmlDoc = new Document();

      Element xmlRoot = new Element();

      Element xmlInstance = new Element();
      xmlInstance.setName("data");

      Element xmlGroup = new Element();
      xmlGroup.setName("group");

      List<Element> xmlFields = IntStream.range(0, values.length).boxed().map(i -> {
        Element xmlField = new Element();
        xmlField.setName("field_" + (i + 1));
        xmlField.addChild(TEXT, values[i]);
        return xmlField;
      }).collect(toList());


      xmlDoc.addChild(ELEMENT, xmlRoot);
      xmlRoot.addChild(ELEMENT, xmlInstance);
      xmlInstance.addChild(ELEMENT, xmlGroup);
      xmlFields.forEach(field -> xmlGroup.addChild(ELEMENT, field));

      return new XmlElement(xmlGroup);
    }

    Path getWorkDir() {
      return workDir;
    }

    Path getOutputMediaDir() {
      return outputMediaDir;
    }

    List<Pair<String, String>> mapSimpleValue(String value) {
      return mapValue(buildSimpleValueSubmission(value), true);
    }

    List<Pair<String, String>> mapSimpleValue(String value, boolean exportMedia) {
      return mapValue(buildSimpleValueSubmission(value), exportMedia);
    }

    List<Pair<String, String>> mapGroupValue(String... values) {
      return mapValue(buildGroupValueSubmission(values), true);
    }

    private List<Pair<String, String>> mapValue(XmlElement value, boolean exportMedia) {
      ExportConfiguration configuration = new ExportConfiguration(
          Optional.of("test_output.csv"),
          Optional.of(getOutputMediaDir().getParent()),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.of(false),
          Optional.empty(),
          Optional.of(true),
          Optional.of(exportMedia),
          Optional.empty(),
          Optional.of(false)
      );
      return CsvFieldMappers
          .getMapper(fieldModel, false)
          .apply(
              instanceId,
              getWorkDir(),
              fieldModel,
              Optional.of(value),
              configuration
          )
          .collect(toList());
    }

    public List<Path> getPaths() {
      return Arrays.asList(workDir, outputMediaDir);
    }
  }
}