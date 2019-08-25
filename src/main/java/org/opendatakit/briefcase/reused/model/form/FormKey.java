package org.opendatakit.briefcase.reused.model.form;

import java.util.Objects;
import java.util.Optional;

public class FormKey {
  private final String id;
  private final Optional<String> version;

  private FormKey(String id, Optional<String> version) {
    this.id = id;
    this.version = version;
  }

  public static FormKey of(String id) {
    return new FormKey(id, Optional.empty());
  }

  public static FormKey of(String id, String version) {
    return new FormKey(id, Optional.of(version));
  }

  public static FormKey of(String id, Optional<String> version) {
    return new FormKey(id, version);
  }

  public String getId() {
    return id;
  }

  public Optional<String> getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormKey formKey = (FormKey) o;
    return Objects.equals(id, formKey.id) &&
        Objects.equals(version, formKey.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version);
  }

  @Override
  public String toString() {
    return "FormKey{" +
        ", id='" + id + '\'' +
        ", version=" + version +
        '}';
  }


}
