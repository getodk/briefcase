package org.opendatakit.briefcase.delivery;

import java.util.Arrays;
import java.util.List;

public class LegacyPreferenceKey {
  private static final List<String> IRRELEVANT_KEY_NAMES = Arrays.asList(
      "tracking warning showed",
      "briefcaseTrackingConsent",
      "uniqueUserID"
  );
  public final String nodeName;
  public final String keyName;

  LegacyPreferenceKey(String nodeName, String keyName) {
    this.nodeName = nodeName;
    this.keyName = keyName;
  }

  @Override
  public String toString() {
    return nodeName + "<" + keyName + ">";
  }

  public boolean hasAppScope() {
    return nodeName.startsWith("/org/opendatakit/briefcase");
  }

  public boolean isRelevant() {
    // Nevermind the "false" here. It's just a trick to automatically align all the conditions
    return !(false
        || IRRELEVANT_KEY_NAMES.contains(keyName)
        || nodeName.startsWith("/org/opendatakit/briefcase/reused/model/preferences")
        || nodeName.startsWith("/org.opendatakit.briefcase.delivery")
        || (nodeName.equals("/org/opendatakit/briefcase/reused") && keyName.equals("saved locations"))
    );
  }
}
