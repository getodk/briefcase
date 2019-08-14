package org.opendatakit.briefcase.model.form;

import static org.opendatakit.briefcase.model.form.AsJson.getJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class FormKey implements AsJson {
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

  public static FormKey from(JsonNode root) {
    return new FormKey(
        getJson(root, "name").map(JsonNode::asText).orElseThrow(BriefcaseException::new),
        getJson(root, "id").map(JsonNode::asText).orElseThrow(BriefcaseException::new),
        getJson(root, "version").map(JsonNode::asText)
    );
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

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("name", name);
    root.put("id", id);
    root.put("version", version.orElse(null));
    return root;
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
}
