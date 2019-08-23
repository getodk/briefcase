package org.opendatakit.briefcase.model.form;

import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.model.FormStatus;

public class FormKey {
  private final String name;
  private final String id;
  private final Optional<String> version;

  private FormKey(String name, String id, Optional<String> version) {
    this.name = name;
    this.id = id;
    this.version = version;
  }

  public static FormKey from(FormStatus formStatus) {
    return new FormKey(
        formStatus.getFormName(),
        formStatus.getFormId(),
        formStatus.getVersion()
    );
  }

  public static FormKey of(String name, String id) {
    return new FormKey(name, id, Optional.empty());
  }

  public static FormKey of(String name, String id, String version) {
    return new FormKey(name, id, Optional.of(version));
  }

  public static FormKey of(String name, String id, Optional<String> version) {
    return new FormKey(name, id, version);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormKey formKey = (FormKey) o;
    return Objects.equals(name, formKey.name) &&
        Objects.equals(id, formKey.id) &&
        Objects.equals(version, formKey.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id, version);
  }

  @Override
  public String toString() {
    return "FormKey{" +
        "name='" + name + '\'' +
        ", id='" + id + '\'' +
        ", version=" + version +
        '}';
  }

  public Path buildFormFile(Path briefcaseDir) {
    return briefcaseDir.resolve("forms").resolve(stripIllegalChars(name)).resolve(stripIllegalChars(name) + ".xml");
  }
}
