package org.opendatakit.briefcase.reused;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.StringUtils.stripIllegalChars;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class Workspace {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SAVED_LOCATIONS_KEY = "saved locations";
  private final List<Consumer<Path>> startCallbacks = new ArrayList<>();
  private final List<Runnable> stopCallbacks = new ArrayList<>();
  private final Preferences prefs;
  private Optional<Path> workspaceLocation = Optional.empty();

  public Workspace(Preferences prefs) {
    this.prefs = prefs;
  }

  public static Workspace empty() {
    return new Workspace(Preferences.userNodeForPackage(Workspace.class));
  }

  public Path get() {
    return workspaceLocation.orElseThrow(() -> new BriefcaseException("Workspace not initialized"));
  }

  private Path getFormsDir() {
    return get().resolve("forms");
  }

  public void startAt(Path workspaceLocation) {
    this.workspaceLocation = Optional.of(workspaceLocation);
    createDirectories(getFormsDir());
    saveLocation(workspaceLocation);
    startCallbacks.forEach(callback -> callback.accept(workspaceLocation));
  }

  public Path buildFormFile(FormMetadata formMetadata) {
    String baseName = stripIllegalChars(formMetadata.getFormName().orElse(formMetadata.getKey().getId()));
    return getFormsDir().resolve(baseName).resolve(baseName + ".xml");
  }

  public Workspace onStart(Consumer<Path> callback) {
    startCallbacks.add(callback);
    return this;
  }

  public Workspace onStop(Runnable callback) {
    stopCallbacks.add(callback);
    return this;
  }

  public void stop() {
    stopCallbacks.forEach(Runnable::run);
  }

  public Set<Path> getSavedLocations() {
    try {
      Set<Path> locations = new LinkedHashSet<>();
      for (JsonNode location : JSON.readTree(prefs.get(SAVED_LOCATIONS_KEY, "[]")))
        locations.add(Paths.get(location.asText()));
      return locations;
    } catch (IOException e) {
      return emptySet();
    }
  }

  private void saveLocation(Path location) {
    try {
      Set<Path> savedLocations = getSavedLocations();
      savedLocations.add(location);
      // Ensure we store the last 5
      if (savedLocations.size() > 5)
        savedLocations = new LinkedHashSet<>(new LinkedList<>(savedLocations).subList(1, 6));
      prefs.put(SAVED_LOCATIONS_KEY, JSON.writeValueAsString(savedLocations.stream().map(Objects::toString).collect(toList())));
    } catch (JsonProcessingException e) {
      throw new BriefcaseException(e);
    }
  }
}
