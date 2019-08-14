package org.opendatakit.briefcase.model.form;

import static org.opendatakit.briefcase.model.form.AsJson.getJson;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class FormMetadata implements AsJson {
  private final FormKey key;
  private final Path storageDirectory;
  private final boolean hasBeenPulled;
  private final Cursor cursor;

  public FormMetadata(FormKey key, Path storageDirectory, boolean hasBeenPulled, Cursor cursor) {
    this.key = key;
    this.storageDirectory = storageDirectory;
    this.hasBeenPulled = hasBeenPulled;
    this.cursor = cursor;
  }

  public static FormMetadata of(FormKey key) {
    // Hardcoded storage directory because we want this class to decide where a
    // form is/should be stored. Now it's based on the Briefcase storage directory,
    // but it will change in the future to be based on a hash of the form key.
    Path storageDirectory = BriefcasePreferences.appScoped()
        .getBriefcaseDir()
        .orElseThrow(BriefcaseException::new)
        .resolve("forms")
        .resolve(stripIllegalChars(key.getName()));
    return new FormMetadata(key, storageDirectory, false, Cursor.empty());
  }

  public static FormMetadata from(Path formFile) {
    XmlElement root = XmlElement.from(formFile);
    assert root.getName().equals("html");
    String name = root.findElements("head", "title").get(0)
        .maybeValue()
        .orElseThrow(BriefcaseException::new);
    XmlElement mainInstance = root.findElements("head", "model", "instance").stream()
        .filter(FormMetadata::isTheMainInstance)
        .findFirst()
        .orElseThrow(BriefcaseException::new);
    String id = mainInstance.childrenOf().get(0).getAttributeValue("id").orElseThrow(BriefcaseException::new);
    Optional<String> version = mainInstance.getAttributeValue("version");
    FormKey key = FormKey.of(name, id, version);
    return new FormMetadata(key, formFile.getParent(), true, Cursor.empty());
  }

  public static FormMetadata from(JsonNode root) {
    return new FormMetadata(
        FormKey.from(root.get("key")),
        getJson(root, "storageDirectory").map(JsonNode::asText).map(Paths::get).orElseThrow(BriefcaseException::new),
        getJson(root, "hasBeenPulled").map(JsonNode::asBoolean).orElseThrow(BriefcaseException::new),
        Cursor.from(root.get("cursor"))
    );
  }

  private static boolean isTheMainInstance(XmlElement e) {
    return !e.hasAttribute("id") // It's not a secondary instance
        && e.childrenOf().size() == 1 // Has only one child (sanity check: an <instance> with more than one children is probably illegal)
        && e.childrenOf().get(0).hasAttribute("id"); // The only child has an id (sanity check: we can't handle forms without form id)
  }

  public FormKey getKey() {
    return key;
  }

  public Path getStorageDirectory() {
    return storageDirectory;
  }

  public Cursor getCursor() {
    return cursor;
  }

  public FormMetadata withCursor(Cursor cursor) {
    return new FormMetadata(key, storageDirectory, hasBeenPulled, cursor);
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.putObject("key").setAll(key.asJson(mapper));
    root.put("storageDirectory", storageDirectory.toAbsolutePath().toString());
    root.put("hasBeenPulled", hasBeenPulled);
    root.putObject("cursor").setAll(cursor.asJson(mapper));
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormMetadata that = (FormMetadata) o;
    return hasBeenPulled == that.hasBeenPulled &&
        Objects.equals(key, that.key) &&
        Objects.equals(storageDirectory, that.storageDirectory) &&
        Objects.equals(cursor, that.cursor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, storageDirectory, hasBeenPulled, cursor);
  }

  @Override
  public String toString() {
    return "FormMetadata{" +
        "key=" + key +
        ", storageDirectory=" + storageDirectory +
        ", hasBeenPulled=" + hasBeenPulled +
        ", cursor=" + cursor +
        '}';
  }
}
