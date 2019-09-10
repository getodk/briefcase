package org.opendatakit.briefcase.reused.model.preferences;

public enum PreferenceCategory {
  GLOBAL(""),
  PULL("pull"),
  PUSH("push"),
  EXPORT("export");

  private final String name;

  PreferenceCategory(String name) {
    this.name = name;
  }
}
