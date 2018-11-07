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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.javarosa.core.model.DataType.TEXT;
import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.instance.TreeElement;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class ModelBuilder {
  private final TreeElement current;
  private final Map<String, QuestionDef> controls;

  ModelBuilder(TreeElement current, Map<String, QuestionDef> controls) {
    this.current = current;
    this.controls = controls;
  }

  public static ModelBuilder root(ModelBuilder... children) {
    ModelBuilder root = new ModelBuilder(new TreeElement(null, DEFAULT_MULTIPLICITY), emptyMap());
    root.add(children);
    return root;
  }

  public static ModelBuilder instance(ModelBuilder... fields) {
    return instance(Arrays.asList(fields));
  }

  public static ModelBuilder instance(List<ModelBuilder> fields) {
    ModelBuilder root = new ModelBuilder(new TreeElement(null, DEFAULT_MULTIPLICITY), emptyMap());
    return root.add(group("data", fields));
  }

  public static ModelBuilder field(String name, DataType type) {
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    element.setDataType(type.value);
    return new ModelBuilder(element, emptyMap());
  }

  public static ModelBuilder field(String name, DataType type, QuestionDef control) {
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    element.setDataType(type.value);
    return new ModelBuilder(element, singletonMap(name, control));
  }

  public static ModelBuilder nul(String name) {
    return field(name, DataType.NULL);
  }

  public static ModelBuilder text(String name) {
    return field(name, DataType.TEXT);
  }

  public static ModelBuilder geopoint(String name) {
    return field(name, DataType.GEOPOINT);
  }

  public static ModelBuilder geotrace(String name) {
    return field(name, DataType.GEOTRACE);
  }

  public static ModelBuilder geoshape(String name) {
    return field(name, DataType.GEOSHAPE);
  }

  public static ModelBuilder selectMultiple(String name, SelectChoice... choices) {
    QuestionDef control = new QuestionDef();
    control.setControlType(Model.ControlType.SELECT_MULTI.value);
    for (SelectChoice choice : choices)
      control.addSelectChoice(choice);
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    element.setDataType(TEXT.value);
    return new ModelBuilder(element, singletonMap(name, control));
  }


  public static ModelBuilder group(String name, ModelBuilder... children) {
    return group(name, Arrays.asList(children));
  }

  public static ModelBuilder group(String name, List<ModelBuilder> children) {
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    element.setDataType(DataType.NULL.value);
    element.setRepeatable(false);
    ModelBuilder group = new ModelBuilder(element, emptyMap());
    return group.add(children);
  }

  public static ModelBuilder repeat(String name, ModelBuilder... children) {
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    element.setDataType(DataType.NULL.value);
    element.setRepeatable(true);
    ModelBuilder repeat = new ModelBuilder(element, emptyMap());
    return repeat.add(children);
  }

  private static Document parse(String xml) throws XmlPullParserException, IOException {
    Document tempDoc = new Document();
    KXmlParser parser = new KXmlParser();
    parser.setInput(new StringReader(xml));
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    tempDoc.parse(parser);
    return tempDoc;
  }

  static XmlElement parseXmlElement(String xml) throws XmlPullParserException, IOException {
    return XmlElement.of(parse(xml));
  }

  static XmlElement buildXmlElementFrom(Model field) throws IOException, XmlPullParserException {
    Model current = field;
    String xml = "<" + current.getName() + "/>";
    while (current.hasParent() && current.getParent().getName() != null) {
      current = current.getParent();
      xml = "<" + current.getName() + ">" + xml + "</" + current.getName() + ">";
    }
    return parseXmlElement(xml);
  }

  public ModelBuilder add(ModelBuilder... children) {
    return add(Arrays.asList(children));
  }

  public ModelBuilder add(List<ModelBuilder> children) {
    TreeElement newCurrent = copy(current);
    Map<String, QuestionDef> newControls = new HashMap<>();
    controls.forEach(newControls::put);
    for (ModelBuilder child : children) {
      TreeElement newChild = copy(child.current);
      newCurrent.addChild(child.current);
      newChild.setParent(newCurrent);
      child.controls.forEach(newControls::put);
    }
    return new ModelBuilder(newCurrent, newControls);
  }

  private static TreeElement copy(TreeElement element) {
    TreeElement newElement = new TreeElement(element.getName(), element.getMultiplicity());
    newElement.setDataType(element.getDataType());
    newElement.setRepeatable(element.isRepeatable());
    return newElement;
  }

  Model build() {
    return new Model(current, controls);
  }

  public String getName() {
    return current.getName();
  }
}
