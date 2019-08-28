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
package org.opendatakit.briefcase.reused.model;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.javarosa.xform.parse.XFormParser.getXMLText;
import static org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.form.FormModel;
import org.xmlpull.v1.XmlPullParserException;

public class XmlElement {
  private final Element element;

  public XmlElement(Element element) {
    this.element = element;
  }

  /**
   * Returns a new XmlElement representing the root of the provided document
   */
  public static XmlElement of(Document document) {
    return new XmlElement(document.getRootElement());
  }

  /**
   * Parses the provided plain text xml document and returns a new XmlElement
   * representing the root element of the xml
   */
  public static XmlElement from(String xml) {
    try {
      Document tempDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(new StringReader(xml));
      parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
      tempDoc.parse(parser);
      return XmlElement.of(tempDoc);
    } catch (XmlPullParserException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  /**
   * Parses the provided xml file and returns a new XmlElement
   * representing the root element of the xml
   */
  public static XmlElement from(Path xmlFile) {
    try (InputStream is = UncheckedFiles.newInputStream(xmlFile);
         InputStreamReader isr = new InputStreamReader(is, UTF_8)) {
      Document tempDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
      tempDoc.parse(parser);
      return XmlElement.of(tempDoc);
    } catch (IOException | XmlPullParserException e) {
      throw new BriefcaseException(e);
    }
  }

  /**
   * Builds and returns this {@link XmlElement} instance's parent's local ID.
   * This ID is used to cross-reference values in different exported files.
   */
  // TODO Make static and move this to the export package
  public String getParentLocalId(FormModel field, String instanceId) {
    return isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(field.getParent(), instanceId);
  }

  /**
   * Builds and returns this {@link XmlElement} instance's current local ID.
   * This ID is used to cross-reference values in different exported files.
   */
  // TODO Make static and move this to the export package
  public String getCurrentLocalId(FormModel field, String instanceId) {
    String prefix = isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(field.getParent(), instanceId);
    return field.isRepeatable()
        ? prefix + "/" + getName() + "[" + getPlaceAmongSameTagSiblings() + "]"
        : prefix;
  }

  /**
   * Builds and returns this {@link XmlElement} instance's group local ID.
   * This ID is used to cross-reference values in different exported files.
   */
  // TODO Make static and move this to the export package
  public String getGroupLocalId(FormModel field, String instanceId) {
    String prefix = isFirstLevelGroup() ? instanceId : getParent().getCurrentLocalId(field.getParent(), instanceId);
    return prefix + "/" + getName();
  }

  /**
   * Returns an index map with all the descendants of this element grouped by their FQN
   */
  public Map<String, List<XmlElement>> buildDescendantsIndex() {
    return flatten().collect(groupingBy(XmlElement::fqn));
  }

  /**
   * Returns the first element with the given name among this element's descendants.
   */
  public Optional<XmlElement> findFirstElement(String name) {
    return flatten().filter(e -> e.hasName(name)).findFirst();
  }

  /**
   * Compiles and returns the list of elements following a given list of names to follow.
   * <p>
   * If the list of names contains only a name, the list will contain the children of some child
   * element of this instance with the given name.
   * <p>
   * If the list of names contains more than one names, the list will be obtained by calling
   * recursively to this method while traversing this instance's descendants following the given list
   * of names.
   */
  public List<XmlElement> findElements(String... namesArray) {
    // Terminal case: there's only one name in the names array
    if (namesArray.length == 1)
      return childrenOf().stream().filter(e -> e.getName().equals(namesArray[0])).collect(Collectors.toList());

    // Recursive case: take the first name, and call again with the tail of the names array
    List<String> names = Arrays.asList(namesArray);
    return findFirstElement(names.get(0))
        .map(e -> e.findElements(names.subList(1, names.size()).toArray(new String[]{})))
        .orElse(Collections.emptyList());
  }

  /**
   * Returns the value of this element or throws when there's no value present.
   */
  public String getValue() {
    return maybeValue().orElseThrow(() -> new BriefcaseException("No value present on element " + element.getName()));
  }

  public Optional<String> maybeValue() {
    return Optional.ofNullable(getXMLText(element, true))
        .filter(s -> !s.isEmpty());
  }

  /**
   * Returns true if this element has an attribute with the given name, false otherwise
   */
  public boolean hasAttribute(String name) {
    for (int i = 0, max = element.getAttributeCount(); i < max; i++)
      if (element.getAttributeName(i).equals(name))
        return true;
    return false;
  }

  /**
   * Returns the value of an attribute of this element.
   */
  public Optional<String> getAttributeValue(String attribute) {
    return Optional.ofNullable(element.getAttributeValue(null, attribute)).filter(s -> !s.isEmpty());
  }

  /**
   * Returns the Fully Qualified Name of this element, which
   * is the concatenation of this instance's name and all its
   * ancestors' names.
   */
  public String fqn() {
    return fqn("");
  }

  /**
   * Returns the Fully Qualified Name of this element, having
   * prefixed a given base name to it.
   */
  private String fqn(String prefix) {
    String newBase = prefix.isEmpty() ? element.getName() : element.getName() + "-" + prefix;
    XmlElement parent = getParent();
    if (!parent.isFirstLevelNode())
      return parent.fqn(newBase);
    return newBase;
  }

  public String getName() {
    return element.getName();
  }

  public boolean isEmpty() {
    return maybeValue().isEmpty();
  }

  private XmlElement getParent() {
    return new XmlElement((Element) element.getParent());
  }

  private Stream<XmlElement> flatten() {
    return childrenOf().stream().flatMap(e -> e.size() == 0
        ? Stream.of(e)
        : Stream.concat(Stream.of(e), e.flatten()));
  }

  public List<XmlElement> childrenOf() {
    List<XmlElement> children = new ArrayList<>();
    for (int i = 0, max = size(); i < max; i++)
      if (element.getType(i) == Node.ELEMENT)
        children.add(new XmlElement(element.getElement(i)));
    return children;
  }

  /**
   * Returns an XML string representing this element.
   */
  public String serialize() {
    StringWriter stringWriter = new StringWriter();
    KXmlSerializer serializer = new KXmlSerializer();
    element.setPrefix(null, "http://opendatakit.org/submissions");
    try {
      serializer.setOutput(stringWriter);
      element.write(serializer);
      serializer.flush();
      serializer.endDocument();
      stringWriter.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return stringWriter.toString();
  }

  private boolean hasName(String name) {
    return element.getName().equals(name);
  }

  private boolean isFirstLevelGroup() {
    return getParent().isFirstLevelNode();
  }

  private int size() {
    return element.getChildCount();
  }

  private List<XmlElement> siblings() {
    return getParent().childrenOf();
  }

  private int getPlaceAmongSameTagSiblings() {
    List<XmlElement> sameTagSiblings = siblings().stream()
        .filter(e -> e.hasName(element.getName()))
        .collect(toList());
    for (int index = 0; index < sameTagSiblings.size(); index++)
      if (sameTagSiblings.get(index).equals(this))
        return index + 1;
    throw new BriefcaseException("Element not found");
  }

  private boolean isFirstLevelNode() {
    return element.getParent() instanceof Document;
  }

  @Override
  public String toString() {
    return "<" + element.getName() + ">";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    XmlElement that = (XmlElement) o;
    return Objects.equals(element, that.element);
  }

  @Override
  public int hashCode() {
    return Objects.hash(element);
  }
}
