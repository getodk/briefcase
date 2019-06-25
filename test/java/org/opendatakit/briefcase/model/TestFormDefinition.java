package org.opendatakit.briefcase.model;

/**
 * Test formdef class with the bare minimum required things to work
 */
public class TestFormDefinition implements IFormDefinition {

  private final String name;
  private final String id;
  private final String version;

  public TestFormDefinition(String name, String id, String version) {
    this.name = name;
    this.id = id;
    this.version = version;
  }

  @Override
  public String getFormName() {
    return name;
  }

  @Override
  public String getFormId() {
    return id;
  }

  @Override
  public String getVersionString() {
    return version;
  }
}
