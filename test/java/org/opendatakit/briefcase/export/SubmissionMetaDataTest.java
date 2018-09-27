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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SubmissionMetaDataTest {
  @Test
  public void gets_media_names() throws IOException, XmlPullParserException {
    // This form we're going to parse has two media nodes with one file on each of them
    // This produced a bug reported at https://github.com/opendatakit/briefcase/issues/653
    Path path = getPath("encrypted-form-media-submission.xml");
    String xml = new String(Files.readAllBytes(path), UTF_8);
    SubmissionMetaData smd = new SubmissionMetaData(XmlElement.of(parse(xml)));
    assertThat(smd.getMediaNames(), hasSize(2));
  }

  public Path getPath(String fileName) {
    URL resource = ExportToCsvTest.class.getClassLoader().getResource("org/opendatakit/briefcase/export/" + fileName);
    return Paths.get(toURI(resource));
  }

  private static Document parse(String xml) throws XmlPullParserException, IOException {
    Document doc = new Document();
    KXmlParser parser = new KXmlParser();
    parser.setInput(new StringReader(xml));
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    doc.parse(parser);
    return doc;
  }
}