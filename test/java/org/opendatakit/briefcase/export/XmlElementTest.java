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
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlElementTest {
  @Test
  public void knows_how_to_generate_keys_of_a_repeat_nested_in_groups() throws IOException, XmlPullParserException {
    Model field = new ModelBuilder()
        .addGroup("g1")
        .addGroup("g2")
        .addGroup("g3")
        .addRepeatGroup("r")
        .build();
    XmlElement xmlElement = buildXmlElementFrom(field);

    assertThat(xmlElement.getCurrentLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID/r[1]"));
    assertThat(xmlElement.getParentLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID"));
    assertThat(xmlElement.getGroupLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID/r"));
  }

  @Test
  public void knows_how_to_generate_keys_of_nested_repeats() throws IOException, XmlPullParserException {
    Model field = new ModelBuilder()
        .addGroup("g1")
        .addGroup("g2")
        .addGroup("g3")
        .addRepeatGroup("r1")
        .addRepeatGroup("r2")
        .build();
    XmlElement xmlElement = buildXmlElementFrom(field);

    assertThat(xmlElement.getCurrentLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID/r1[1]/r2[1]"));
    assertThat(xmlElement.getParentLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID/r1[1]"));
    assertThat(xmlElement.getGroupLocalId(field, "uuid:SOMELONGUUID"), is("uuid:SOMELONGUUID/r1[1]/r2"));
  }

  private static Document parse(String xml) throws XmlPullParserException, IOException {
    Document tempDoc = new Document();
    KXmlParser parser = new KXmlParser();
    parser.setInput(new StringReader(xml));
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    tempDoc.parse(parser);
    return tempDoc;
  }

  private static XmlElement buildXmlElementFrom(Model field) throws IOException, XmlPullParserException {
    Model current = field;
    String xml = "<" + current.getName() + "/>";
    while (current.hasParent()) {
      current = current.getParent();
      xml = "<" + current.getName() + ">" + xml + "</" + current.getName() + ">";
    }
    return XmlElement.of(parse(xml)).findElement(field.getName()).get();
  }
}