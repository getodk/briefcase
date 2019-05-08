/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.io.FileUtils;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.aggregate.form.XFormParameters;
import org.opendatakit.briefcase.model.CannotFixXMLException;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.ParsingException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

@SuppressWarnings("checkstyle:MissingSwitchDefault")
class XmlManipulationUtils {

  // NOTE: the only transfered metadata is the instanceID and the submissionDate

  private static final String INSTANCE_ID_ATTRIBUTE_NAME = "instanceID";

  private static final String OPEN_ROSA_NAMESPACE_PRELIM = "http://openrosa.org/xforms/metadata";
  private static final String OPEN_ROSA_NAMESPACE = "http://openrosa.org/xforms";
  private static final String OPEN_ROSA_NAMESPACE_SLASH = "http://openrosa.org/xforms/";
  private static final String OPEN_ROSA_METADATA_TAG = "meta";
  private static final String OPEN_ROSA_INSTANCE_ID = "instanceID";
  private static final String BASE64_ENCRYPTED_FIELD_KEY = "base64EncryptedFieldKey";


  /**
   * Traverse submission looking for OpenRosa metadata tag (with or without
   * namespace).
   */
  private static Element findMetaTag(Element parent, String rootUri) {
    for (int i = 0; i < parent.getChildCount(); ++i) {
      if (parent.getType(i) == Node.ELEMENT) {
        Element child = parent.getElement(i);
        String cnUri = child.getNamespace();
        String cnName = child.getName();
        if (cnName.equals(OPEN_ROSA_METADATA_TAG)
            && (cnUri == null ||
            cnUri.equals(EMPTY_STRING) ||
            cnUri.equals(rootUri) ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE) ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_SLASH) ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_PRELIM))) {
          return child;
        } else {
          Element descendent = findMetaTag(child, rootUri);
          if (descendent != null)
            return descendent;
        }
      }
    }
    return null;
  }

  /**
   * Find the OpenRosa instanceID defined for this record, if any.
   */
  private static String getOpenRosaInstanceId(Element root) {
    String rootUri = root.getNamespace();
    Element meta = findMetaTag(root, rootUri);
    if (meta != null) {
      for (int i = 0; i < meta.getChildCount(); ++i) {
        if (meta.getType(i) == Node.ELEMENT) {
          Element child = meta.getElement(i);
          String cnUri = child.getNamespace();
          String cnName = child.getName();
          if (cnName.equals(OPEN_ROSA_INSTANCE_ID)
              && (cnUri == null ||
              cnUri.equals(EMPTY_STRING) ||
              cnUri.equals(rootUri) ||
              cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE) ||
              cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_SLASH) ||
              cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_PRELIM))) {
            return XFormParser.getXMLText(child, true);
          }
        }
      }
    }
    return null;
  }

  /**
   * Encrypted field-level encryption key.
   */
  private static String getBase64EncryptedFieldKey(Element root) {
    String rootUri = root.getNamespace();
    Element meta = findMetaTag(root, rootUri);
    if (meta != null) {
      for (int i = 0; i < meta.getChildCount(); ++i) {
        if (meta.getType(i) == Node.ELEMENT) {
          Element child = meta.getElement(i);
          String cnUri = child.getNamespace();
          String cnName = child.getName();
          if (cnName.equals(BASE64_ENCRYPTED_FIELD_KEY)
              && (cnUri == null ||
              cnUri.equals(EMPTY_STRING) ||
              cnUri.equals(rootUri) ||
              cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE) ||
              cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_SLASH))) {
            return XFormParser.getXMLText(child, true);
          }
        }
      }
    }
    return null;
  }

  public static class FormInstanceMetadata {
    final XFormParameters xparam;
    public final String instanceId; // this may be null
    final String base64EncryptedFieldKey; // this may be null

    FormInstanceMetadata(XFormParameters xparam, String instanceId, String base64EncryptedFieldKey) {
      this.xparam = xparam;
      this.instanceId = instanceId;
      this.base64EncryptedFieldKey = base64EncryptedFieldKey;
    }
  }

  private static final String FORM_ID_ATTRIBUTE_NAME = "id";
  private static final String EMPTY_STRING = "";
  private static final String NAMESPACE_ATTRIBUTE = "xmlns";
  private static final String MODEL_VERSION_ATTRIBUTE_NAME = "version";

  static FormInstanceMetadata getFormInstanceMetadata(Element root) throws ParsingException {
    // check for odk id
    String formId = root.getAttributeValue(null, FORM_ID_ATTRIBUTE_NAME);

    // if odk id is not present use namespace
    if (formId == null || formId.equalsIgnoreCase(EMPTY_STRING)) {
      String schema = root.getAttributeValue(null, NAMESPACE_ATTRIBUTE);

      // TODO: move this into FormDefinition?
      if (schema == null) {
        throw new ParsingException("Unable to extract form id");
      }

      formId = schema;
    }

    String modelVersionString = root.getAttributeValue(null, MODEL_VERSION_ATTRIBUTE_NAME);

    String instanceId = getOpenRosaInstanceId(root);
    if (instanceId == null) {
      instanceId = root.getAttributeValue(null, INSTANCE_ID_ATTRIBUTE_NAME);
    }
    String base64EncryptedFieldKey = getBase64EncryptedFieldKey(root);
    return new FormInstanceMetadata(new XFormParameters(formId, modelVersionString), instanceId, base64EncryptedFieldKey);
  }

  static Document parseXml(File submission) throws ParsingException, FileSystemException {
    // parse the xml document...
    Document doc;
    try (InputStream is = new FileInputStream(submission); InputStreamReader isr = new InputStreamReader(is, UTF_8)) {
      Document tempDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      tempDoc.parse(parser);
      doc = tempDoc;
    } catch (XmlPullParserException e) {
      try {
        return BadXMLFixer.fixBadXML(submission);
      } catch (CannotFixXMLException e1) {
        // We just place the debug file in the same folder as the submission we're processing
        File debugFileLocation = submission.toPath().resolveSibling(submission.toPath().getFileName().toString() + ".debug").toFile();
        try {
          if (!debugFileLocation.exists()) {
            FileUtils.forceMkdir(debugFileLocation);
          }
          long checksum = FileUtils.checksumCRC32(submission);
          File debugFile = new File(debugFileLocation, "submission-" + checksum + ".xml");
          FileUtils.copyFile(submission, debugFile);
        } catch (IOException e2) {
          throw new RuntimeException(e2);
        }
        throw new ParsingException("Failed during parsing of submission Xml: "
            + e.toString());
      }
    } catch (IOException e) {
      throw new FileSystemException("Failed while reading submission xml: "
          + e.toString());
    }
    return doc;
  }

}
