package org.opendatakit.briefcase.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendatakit.briefcase.util.XmlManipulationUtils.*;

import org.junit.Before;

import org.junit.Test;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

public class XmlManipulationUtilsTest {

  private static final String ROOT_URI = "http://www.w3.org/2002/xforms";
  private static final String OPEN_ROSA_NAMESPACE = "http://openrosa.org/xforms";
  private static final String OPEN_ROSA_METADATA_TAG = "meta";
  private static final String OPEN_ROSA_INSTANCE_ID = "instanceID";
  private static final String NAMESPACE_ATTRIBUTE = "xmlns";
  private Element tree;

  @Before
  public void setUp(){
    tree = new Element().createElement(null, "html");
    tree.setAttribute(null, NAMESPACE_ATTRIBUTE, ROOT_URI);
    Element head = new Element().createElement(null, "head");
    Element title = new Element().createElement(null, "title");
    Element model = new Element().createElement(null, "model");
    Element instance = new Element().createElement(null, "instance");
    Element data = new Element().createElement(null, "data");
    Element meta = new Element().createElement(OPEN_ROSA_NAMESPACE, OPEN_ROSA_METADATA_TAG);
    Element instanceId = new Element().createElement(OPEN_ROSA_NAMESPACE, OPEN_ROSA_INSTANCE_ID);
    tree.addChild(Node.ELEMENT, head);
    head.addChild(Node.ELEMENT, title);
    head.addChild(Node.ELEMENT, model);
    model.addChild(Node.ELEMENT, instance);
    instance.addChild(Node.ELEMENT, data);
    data.addChild(Node.ELEMENT, meta);
    meta.addChild(Node.ELEMENT, instanceId);
  }

  @Test
  public void find_meta_tag_successful() {
    FormInstanceMetadata metadata = getFormInstanceMetadata(tree);
    assertNotNull(metadata);
  }

}
