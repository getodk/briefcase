package org.opendatakit.briefcase.reused.api;

import static java.util.function.Predicate.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class Json {
  private static final ObjectMapper JSON = new ObjectMapper();

  public static ObjectMapper getMapper() {
    return JSON;
  }

  public static String serialize(JsonNode root) {
    try {
      return JSON.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new BriefcaseException("Can't serialize JSON", e);
    }
  }

  public static JsonNode deserialize(String jsonString) {
    try {
      return JSON.readTree(jsonString);
    } catch (IOException e) {
      throw new BriefcaseException("Can't serialize JSON", e);
    }
  }

  public static Optional<JsonNode> get(JsonNode node, String propertyName) {
    return Optional.ofNullable(node.path(propertyName)).filter(not(JsonNode::isNull));
  }

  public static Optional<String> asText(JsonNode node) {
    return Optional.ofNullable(node).filter(not(JsonNode::isNull)).map(JsonNode::asText);
  }
}
