package org.opendatakit.briefcase.delivery;

class LegacyPreferenceKey {
  private final String nodeName;
  private final String keyName;

  LegacyPreferenceKey(String nodeName, String keyName) {
    this.nodeName = nodeName;
    this.keyName = keyName;
  }

  @Override
  public String toString() {
    return nodeName + "<" + keyName + ">";
  }
}
