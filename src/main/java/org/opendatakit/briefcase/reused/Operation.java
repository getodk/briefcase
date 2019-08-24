package org.opendatakit.briefcase.reused;

public enum Operation {
  PUSH("Push"),
  PULL("Pull"),
  EXPORT("Export");

  private final String name;

  Operation(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
