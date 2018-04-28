package org.opendatakit.briefcase.util;

import static org.javarosa.xform.parse.XFormParser.getXMLText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.aggregate.form.XFormParameters;
import org.opendatakit.briefcase.model.ParsingException;

public class FormInstanceMetadata {
  public final XFormParameters xparam;
  public final Optional<String> instanceId;
  public final Optional<String> base64EncryptedFieldKey;

  FormInstanceMetadata(XFormParameters xparam, Optional<String> instanceId, Optional<String> base64EncryptedFieldKey) {
    this.xparam = xparam;
    this.instanceId = instanceId;
    this.base64EncryptedFieldKey = base64EncryptedFieldKey;
  }

  public static FormInstanceMetadata from(Element root) {
    return new FormInstanceMetadata(
        new XFormParameters(
            getFormId(root),
            getModelVersion(root).orElse(null)
        ),
        getInstanceId(root),
        getBase64EncryptedFieldKeyValue(root)
    );
  }

  public static String getFormId(Element root) {
    return race(
        getAttributeValue(root, "id"),
        getAttributeValue(root, "xmlns")
    ).orElseThrow(() -> new ParsingException("Unable to extract form id"));
  }

  public static Optional<String> getModelVersion(Element root) {
    return getAttributeValue(root, "version");
  }

  public static Optional<String> getInstanceId(Element root) {
    return race(
        getOpenRosaInstanceId(root),
        getAttributeValue(root, "instanceID")
    );
  }

  public static Optional<String> getOpenRosaInstanceId(Element root) {
    return findTag(root, root.getNamespace(), "instanceID")
        .map(FormInstanceMetadata::readAndTrim);
  }

  public static Optional<String> getBase64EncryptedFieldKeyValue(Element root) {
    return findTag(root, root.getNamespace(), "base64EncryptedFieldKey")
        .map(FormInstanceMetadata::readAndTrim);
  }

  public static Optional<String> getBase64EncryptedKey(Element root) {
    return findTag(root, root.getNamespace(), "base64EncryptedKey")
        .map(FormInstanceMetadata::readAndTrim);
  }

  private static String readAndTrim(Element element) {
    return getXMLText(element, true);
  }

  private static Optional<Element> findTag(Element element, String rootNamespace, String tag) {
    for (Element child : childrenOf(element)) {
      return isTag(child, tag, rootNamespace)
          ? Optional.of(child)
          : findTag(child, rootNamespace, tag);
    }
    return Optional.empty();
  }

  private static List<Element> childrenOf(Element element) {
    List<Element> children = new ArrayList<>();
    for (int i = 0, max = element.getChildCount(); i < max; i++) {
      if (element.getType(i) == Node.ELEMENT)
        children.add(element.getElement(i));
    }
    return children;
  }

  private static boolean isTag(Element child, String tag, String rootNamespace) {
    return child.getName().equals(tag) && namespaceIsOk(child.getNamespace(), rootNamespace);
  }

  private static boolean namespaceIsOk(String childNamespace, String rootNamespace) {
    return childNamespace == null
        || childNamespace.isEmpty()
        || childNamespace.equals(rootNamespace)
        || childNamespace.equalsIgnoreCase("http://openrosa.org/xforms")
        || childNamespace.equalsIgnoreCase("http://openrosa.org/xforms/")
        || childNamespace.equalsIgnoreCase("http://openrosa.org/xforms/metadata");
  }

  @SafeVarargs
  private static <T> Optional<T> race(Optional<T>... optionals) {
    return Arrays.stream(optionals)
        .filter(Optional::isPresent)
        .findFirst()
        .flatMap(o -> o);
  }

  public static Optional<String> getAttributeValue(Element element, String attrName) {
    return Optional.ofNullable(element.getAttributeValue(null, attrName)).filter(s -> !s.isEmpty());
  }
}
