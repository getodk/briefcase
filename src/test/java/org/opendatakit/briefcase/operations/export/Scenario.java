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

package org.opendatakit.briefcase.operations.export;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.Collectors.toList;
import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;
import static org.kxml2.kdom.Node.ELEMENT;
import static org.kxml2.kdom.Node.TEXT;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.write;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.instance.TreeElement;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormModel;

class Scenario {
  private static int instanceIdSeq = 1;
  private final Path workDir = createTempDirectory("briefcase_test_workdir");
  private final Path outputDir = createTempDirectory("export_output");
  private final Path outputMediaDir = UncheckedFiles.createDirectories(outputDir.resolve("media"));
  private final String formName;
  private String instanceId;
  private final String instanceName;
  private final String fieldName;
  private final FormModel fieldModel;

  Scenario(String formName, String instanceId, String instanceName, String fieldName, FormModel fieldModel) {
    this.formName = formName;
    this.instanceId = instanceId;
    this.instanceName = instanceName;
    this.fieldName = fieldName;
    this.fieldModel = fieldModel;

    // Side effects:
    // - ExportToCSV ensures that there is an audit output
    //   CSV file with at least the headers on it.
    write(outputDir.resolve(formName + " - audit.csv"), "instance ID, event, node, start, end\n", CREATE);
  }

  static Scenario nonGroup(DataType dataType) {
    return nonGroup("some-form", dataType, "field", null);
  }

  static Scenario nonGroup(DataType dataType, String fieldName) {
    return nonGroup("some-form", dataType, fieldName, null);
  }

  static Scenario nonGroup(String formName, DataType dataType, String fieldName, String parentName) {
    FormModel fieldModel = createField(dataType, fieldName, parentName);
    return new Scenario(formName, "instance_" + instanceIdSeq++, "data", fieldName, fieldModel);
  }

  static FormModel createField(DataType dataType) {
    return createField(dataType, "field", null);
  }

  static FormModel createField(DataType dataType, String fieldName) {
    return createField(dataType, fieldName, null);
  }

  static FormModel createField(DataType dataType, String fieldName, String parentName) {
    List<TreeElement> elements = new LinkedList<>();
    TreeElement fieldTreeElement = new TreeElement(fieldName, DEFAULT_MULTIPLICITY);
    fieldTreeElement.setDataType(dataType.value);
    elements.add(fieldTreeElement);

    if (parentName != null) {
      TreeElement parentTreeElement = new TreeElement(parentName, DEFAULT_MULTIPLICITY);
      parentTreeElement.setDataType(DataType.NULL.value);
      elements.add(parentTreeElement);
    }

    TreeElement instanceTreeElement = new TreeElement("data", DEFAULT_MULTIPLICITY);
    instanceTreeElement.setDataType(DataType.NULL.value);
    elements.add(instanceTreeElement);

    TreeElement rootTreeElement = new TreeElement("/", DEFAULT_MULTIPLICITY);
    rootTreeElement.setDataType(DataType.NULL.value);
    elements.add(rootTreeElement);

    int maxIndex = elements.size() - 1;
    for (int i = 0; i < maxIndex; i++)
      elements.get(i).setParent(elements.get(i + 1));
    for (int i = maxIndex; i > 0; i--)
      elements.get(i).addChild(elements.get(i - 1));

    return new FormModel(fieldTreeElement, Collections.emptyMap());
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

    return new Scenario("some-form", instanceId, "data", "group", new FormModel(groupTreeElement, Collections.emptyMap()));
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

  String getFormName() {
    return formName;
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
    ExportConfiguration configuration = ExportConfiguration.Builder.empty()
        .setExportFilename("test_output.csv")
        .setExportDir(getOutputMediaDir().getParent())
        .setExportAttachments(exportMedia)
        .build();
    return CsvFieldMappers
        .getMapper(fieldModel, false)
        .apply(
            formName,
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

  public Path getOutputDir() {
    return outputDir;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void nextSubmission() {
    instanceId = "instance_" + instanceIdSeq++;
  }
}
