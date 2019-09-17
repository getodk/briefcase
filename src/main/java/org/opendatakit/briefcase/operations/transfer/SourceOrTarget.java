package org.opendatakit.briefcase.operations.transfer;

import static org.opendatakit.briefcase.reused.api.Json.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PathSourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public interface SourceOrTarget {

  static SourceOrTarget from(JsonNode root) {
    Type type = get(root, "type")
        .map(JsonNode::asText)
        .map(SourceOrTarget.Type::from)
        .orElseThrow(BriefcaseException::new);
    if (type == Type.AGGREGATE)
      return AggregateServer.from(root);
    if (type == Type.CENTRAL)
      return CentralServer.from(root);
    if (type == Type.COLLECT_DIRECTORY)
      return PathSourceOrTarget.from(root, Type.COLLECT_DIRECTORY);
    if (type == Type.FORM_DEFINITION)
      return PathSourceOrTarget.from(root, Type.FORM_DEFINITION);
    return null;
  }

  ObjectNode asJson(ObjectMapper mapper);

  SourceOrTarget.Type getType();

  enum Type {
    AGGREGATE("aggregate"),
    CENTRAL("central"),
    COLLECT_DIRECTORY("collect directory"),
    FORM_DEFINITION("form definition");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    public static Type from(String name) {
      return Stream.of(values())
          .filter(v -> v.name.equals(name))
          .findFirst()
          .orElseThrow();
    }

    public String getName() {
      return name;
    }
  }
}
