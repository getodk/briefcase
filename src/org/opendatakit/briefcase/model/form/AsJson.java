package org.opendatakit.briefcase.model.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;

public interface AsJson {
  static Optional<JsonNode> getJson(JsonNode root, String propertyName) {
    return Optional.ofNullable(root.get(propertyName)).filter(node -> !node.isNull());
  }

  ObjectNode asJson(ObjectMapper mapper);
}
