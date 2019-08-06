package org.opendatakit.briefcase.pull.aggregate;

import java.util.UUID;

class CursorHelpers {
  static String buildCursorXml(String lastUpdate) {
    return buildCursorXml(lastUpdate, UUID.randomUUID().toString());
  }

  static String buildCursorXml(String lastUpdate, String lastId) {
    return "" +
        "<cursor xmlns=\"http://www.opendatakit.org/cursor\">\n" +
        "<attributeName>_LAST_UPDATE_DATE</attributeName>\n" +
        "<attributeValue>" + lastUpdate + "</attributeValue>\n" +
        "<uriLastReturnedValue>" + lastId + "</uriLastReturnedValue>\n" +
        "<isForwardCursor>true</isForwardCursor>\n" +
        "</cursor>" +
        "";
  }
}
