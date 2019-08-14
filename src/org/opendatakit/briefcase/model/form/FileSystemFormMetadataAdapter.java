package org.opendatakit.briefcase.model.form;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.toMap;
import static org.opendatakit.briefcase.reused.UncheckedFiles.delete;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.LegacyPrefs;

public class FileSystemFormMetadataAdapter implements FormMetadataPort {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private final Map<FormKey, FormMetadata> store = new ConcurrentHashMap<>();

  public static FormMetadataPort at(Path storageRoot) {
    return new FileSystemFormMetadataAdapter().syncWithFilesAt(storageRoot);
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    command.accept(this);
  }

  public FormMetadataPort syncWithFilesAt(Path storageRoot) {
    flush();
    store.putAll(walk(storageRoot.resolve("forms"))

        // select XML files that are not submissions
        .filter(path -> !path.getFileName().toString().equals("submission.xml")
            && path.getFileName().toString().endsWith(".xml"))

        // select XML files that look like forms by parsing them
        // and looking for key parts that all forms must have
        .filter(path -> isAForm(XmlElement.from(path)))

        // Parse existing metadata.json files or build new FormMetadata from form files
        .map(formFile -> {
          Path formDir = formFile.getParent();
          Path metadataFile = formDir.resolve("metadata.json");
          return Files.exists(metadataFile) ? deserialize(metadataFile) : FormMetadata.from(formFile);
        })

        // Try to recover any missing cursor from the legacy Java prefs system
        .map(metadata -> {
          if (!metadata.getCursor().isEmpty())
            return metadata;
          return LegacyPrefs.readCursor(metadata.getKey().getId())
              .map(metadata::withCursor)
              .orElse(metadata);
        })

        // Write updated metadata.json files
        .peek(this::persist)

        .collect(toMap(FormMetadata::getKey, metadata -> metadata)));
    return this;
  }

  private boolean isAForm(XmlElement root) {
    return root.getName().equals("html")
        && root.findElements("head", "title").size() == 1
        && root.findElements("head", "model", "instance").size() >= 1
        && root.findElements("body").size() == 1;
  }

  @Override
  public void flush() {
    store.values().forEach(metadata -> delete(getMetadataFile(metadata)));
    store.clear();
  }

  @Override
  public void persist(FormMetadata formMetadata) {
    store.put(formMetadata.getKey(), formMetadata);
    serialize(formMetadata);
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return Optional.ofNullable(store.get(key));
  }

  // region Path <-> JSON <-> FormMetadata serialization
  private static FormMetadata deserialize(Path metadataFile) {
    JsonNode root = uncheckedReadTree(metadataFile);
    return FormMetadata.from(root);
  }

  private static Path serialize(FormMetadata metaData) {
    try {
      return write(
          getMetadataFile(metaData),
          MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(metaData.asJson(MAPPER)),
          CREATE, TRUNCATE_EXISTING
      );
    } catch (JsonProcessingException e) {
      throw new BriefcaseException("Couldn't produce JSON FormMetadata", e);
    }
  }

  private static JsonNode uncheckedReadTree(Path jsonFile) {
    try {
      return MAPPER.readTree(jsonFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Path getMetadataFile(FormMetadata metaData) {
    return metaData.getStorageDirectory().resolve("metadata.json");
  }
  // endregion
}
