package org.opendatakit.briefcase.reused.model.preferences;

import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.BriefcaseException;

public enum PreferenceCategory {
  GLOBAL(""),
  PULL("pull"),
  PUSH("push"),
  EXPORT("export");

  private final String name;

  PreferenceCategory(String name) {
    this.name = name;
  }

  public static PreferenceCategory from(String name) {
    return Stream.of(values())
        .filter(v -> v.name.equals(name))
        .findFirst()
        .orElseThrow(BriefcaseException::new);
  }

  public String getName() {
    return name;
  }
}
