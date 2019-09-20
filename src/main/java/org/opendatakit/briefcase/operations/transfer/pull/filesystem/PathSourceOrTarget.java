package org.opendatakit.briefcase.operations.transfer.pull.filesystem;

import static org.opendatakit.briefcase.reused.api.Json.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class PathSourceOrTarget implements SourceOrTarget {
  private final Path path;
  private final Type type;

  private PathSourceOrTarget(Path path, Type type) {
    this.path = path;
    this.type = type;
  }

  public static PathSourceOrTarget from(JsonNode root, Type type) {
    return new PathSourceOrTarget(
        get(root, "path").map(JsonNode::asText).map(Paths::get).orElseThrow(BriefcaseException::new),
        type
    );
  }

  public static PathSourceOrTarget formDefinitionAt(Path path) {
    return new PathSourceOrTarget(path, Type.FORM_DEFINITION);
  }

  public static PathSourceOrTarget collectFormAt(Path path) {
    return new PathSourceOrTarget(path, Type.COLLECT_DIRECTORY);
  }

  public Path getPath() {
    return path;
  }

  public boolean exists() {
    return Files.exists(path);
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("type", getType().getName());
    root.put("path", path.toString());
    return root;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PathSourceOrTarget that = (PathSourceOrTarget) o;
    return Objects.equals(path, that.path) &&
        type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, type);
  }

  @Override
  public String toString() {
    return path.toString();
  }
}
