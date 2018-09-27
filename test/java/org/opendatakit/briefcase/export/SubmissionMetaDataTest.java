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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SubmissionMetaDataTest {
  @Test
  public void gets_media_names_with_many_files_in_a_media_node() throws IOException, XmlPullParserException {
    String xml = "" +
        "<data id=\"encrypted-form-media\" encrypted=\"yes\" instanceID=\"uuid:482e83f3-fae6-43ba-a6e9-e438ffe348e3\" submissionDate=\"2018-09-27T09:20:53.764Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-09-27T09:20:53.764Z\" xmlns=\"http://opendatakit.org/submissions\">\n" +
        "  <base64EncryptedKey>LQfGcwk6x/+O3BwXHWGROcQr/TeNmA3vHIwB6QA1ezFD9x+83nJHXQYobPSAqy268TtDZr4nQoNHlogpS9KNT3C3zRbMojJEBtCNT8oWFsj0/mJPhqi4gbJlIyCbGCh+VQXVXl9svJHnQrgqkd3XI+Mpfqpwbkf2mfgUl1Im9CHn1RnHh61YxqY0NYvq8q07R/9TvhOk7o8HFWuGTVTYsiay9fnr01Pcc86qGFWHTrDakyNmduoxS2CUrpqMq058rWYaGrFt286xmqFGI/98rOlDowZsJtY/Y/NengykH4+bL+8ZudeQHiY7wq5Ze4VRcMymK4kERXdNmXAZ84uCiQ==</base64EncryptedKey>\n" +
        "  <n0:meta xmlns:n0=\"http://openrosa.org/xforms\">\n" +
        "    <n0:instanceID>uuid:482e83f3-fae6-43ba-a6e9-e438ffe348e3</n0:instanceID>\n" +
        "  </n0:meta>\n" +
        "  <media>\n" +
        "    <file>1538040007350.jpg.enc</file>\n" +
        "    <file>1538040019351.jpg.enc</file>\n" +
        "  </media>\n" +
        "  <encryptedXmlFile>submission.xml.enc</encryptedXmlFile>\n" +
        "  <base64EncryptedElementSignature>5AeMxU8Gr5BGcmCiVec0xfYZ+MPLZoMrsgAZ0DPDNPi5J12L4eHyaRx2oQxNtTgIUePmVu87PTaYrLivM+CUyIQIL6HwQWCjtD+WnmQ5JYBwHZRl6YvRD49vW/UD3Qw1cYeQGpAXbDdia/AEQZvtUhL6FSnQ+F95H11JsBEm6hQqscX5mx60h/n2qhSzSJWbivPjrTfaNL3aYwjHvvIQ/JHTFp7VEV52bUTbtHwBPIZZZZU3Qam1E2wBFFqWchF3qVAMmKJ+wzZZcYqk4AIelRP04gim/rQuF2Y8kyjJMW8HKrXwwZjPgmeiaIiDbWoQhgfhw2n8Qt2OTwSSi8b6ew==</base64EncryptedElementSignature>\n" +
        "</data>" +
        "";
    SubmissionMetaData smd = new SubmissionMetaData(XmlElement.of(parse(xml)));
    assertThat(smd.getMediaNames(), contains("1538040007350.jpg.enc", "1538040019351.jpg.enc"));
  }

  @Test
  public void gets_media_names_with_many_media_nodes_with_one_file() throws IOException, XmlPullParserException {
    String xml = "" +
        "<data id=\"encrypted-form-media\" encrypted=\"yes\" instanceID=\"uuid:482e83f3-fae6-43ba-a6e9-e438ffe348e3\" submissionDate=\"2018-09-27T09:20:53.764Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-09-27T09:20:53.764Z\" xmlns=\"http://opendatakit.org/submissions\">\n" +
        "  <base64EncryptedKey>LQfGcwk6x/+O3BwXHWGROcQr/TeNmA3vHIwB6QA1ezFD9x+83nJHXQYobPSAqy268TtDZr4nQoNHlogpS9KNT3C3zRbMojJEBtCNT8oWFsj0/mJPhqi4gbJlIyCbGCh+VQXVXl9svJHnQrgqkd3XI+Mpfqpwbkf2mfgUl1Im9CHn1RnHh61YxqY0NYvq8q07R/9TvhOk7o8HFWuGTVTYsiay9fnr01Pcc86qGFWHTrDakyNmduoxS2CUrpqMq058rWYaGrFt286xmqFGI/98rOlDowZsJtY/Y/NengykH4+bL+8ZudeQHiY7wq5Ze4VRcMymK4kERXdNmXAZ84uCiQ==</base64EncryptedKey>\n" +
        "  <n0:meta xmlns:n0=\"http://openrosa.org/xforms\">\n" +
        "    <n0:instanceID>uuid:482e83f3-fae6-43ba-a6e9-e438ffe348e3</n0:instanceID>\n" +
        "  </n0:meta>\n" +
        "  <media>\n" +
        "    <file>1538040007350.jpg.enc</file>\n" +
        "  </media>\n" +
        "  <media>\n" +
        "    <file>1538040019351.jpg.enc</file>\n" +
        "  </media>\n" +
        "  <encryptedXmlFile>submission.xml.enc</encryptedXmlFile>\n" +
        "  <base64EncryptedElementSignature>5AeMxU8Gr5BGcmCiVec0xfYZ+MPLZoMrsgAZ0DPDNPi5J12L4eHyaRx2oQxNtTgIUePmVu87PTaYrLivM+CUyIQIL6HwQWCjtD+WnmQ5JYBwHZRl6YvRD49vW/UD3Qw1cYeQGpAXbDdia/AEQZvtUhL6FSnQ+F95H11JsBEm6hQqscX5mx60h/n2qhSzSJWbivPjrTfaNL3aYwjHvvIQ/JHTFp7VEV52bUTbtHwBPIZZZZU3Qam1E2wBFFqWchF3qVAMmKJ+wzZZcYqk4AIelRP04gim/rQuF2Y8kyjJMW8HKrXwwZjPgmeiaIiDbWoQhgfhw2n8Qt2OTwSSi8b6ew==</base64EncryptedElementSignature>\n" +
        "</data>" +
        "";
    SubmissionMetaData smd = new SubmissionMetaData(XmlElement.of(parse(xml)));
    assertThat(smd.getMediaNames(), contains("1538040007350.jpg.enc", "1538040019351.jpg.enc"));
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