package org.opendatakit.briefcase.reused;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.StringUtils.stripIllegalChars;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class Workspace {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SAVED_LOCATIONS_KEY = "saved locations";

  private final Preferences prefs;
  private Optional<Path> workspaceLocation = Optional.empty();

  public Workspace(Preferences prefs) {
    this.prefs = prefs;
  }

  public void setWorkspaceLocation(Path workspaceLocation) {
    this.workspaceLocation = Optional.of(workspaceLocation);
    saveWorkspace();
  }

  public Path get() {
    return workspaceLocation.orElseThrow(() -> new BriefcaseException("Workspace location not set"));
  }

  private Path getFormsDir() {
    return get().resolve("forms");
  }

  public Path buildFormFile(FormMetadata formMetadata) {
    String baseName = stripIllegalChars(formMetadata.getFormName().orElse(formMetadata.getKey().getId()));
    return getFormsDir().resolve(baseName).resolve(baseName + ".xml");
  }

  public Path relativize(Path path) {
    return get().relativize(path);
  }

  public Path resolve(Path path) {
    return get().resolve(path);
  }

  private void saveWorkspace() {
    try {
      Set<Path> savedLocations = getSavedWorkspaces();
      savedLocations.add(get());
      // Ensure we store the last 5
      if (savedLocations.size() > 5)
        savedLocations = new LinkedHashSet<>(new LinkedList<>(savedLocations).subList(1, 6));
      prefs.put(SAVED_LOCATIONS_KEY, JSON.writeValueAsString(savedLocations.stream().map(Objects::toString).collect(toList())));
    } catch (JsonProcessingException e) {
      throw new BriefcaseException(e);
    }
  }

  public Set<Path> getSavedWorkspaces() {
    try {
      Set<Path> locations = new LinkedHashSet<>();
      for (JsonNode location : JSON.readTree(prefs.get(SAVED_LOCATIONS_KEY, "[]")))
        locations.add(Paths.get(location.asText()));
      return locations;
    } catch (IOException e) {
      return emptySet();
    }
  }
}
