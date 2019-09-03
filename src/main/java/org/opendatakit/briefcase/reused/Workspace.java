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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import org.flywaydb.core.Flyway;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;

public class Workspace {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SAVED_LOCATIONS_KEY = "saved locations";
  public final Http http;
  public final BriefcaseVersionManager versionManager;
  private final Preferences prefs;
  private final BriefcaseDb db;
  public final FormMetadataPort formMetadata;
  public final SubmissionMetadataPort submissionMetadata;
  private Optional<Path> workspaceLocation = Optional.empty();

  public Workspace(Http http, BriefcaseVersionManager versionManager, Preferences prefs, BriefcaseDb db, FormMetadataPort formMetadata, SubmissionMetadataPort submissionMetadata) {
    this.http = http;
    this.versionManager = versionManager;
    this.prefs = prefs;
    this.db = db;
    this.formMetadata = formMetadata;
    this.submissionMetadata = submissionMetadata;
  }

  public static Workspace with(Http http, BriefcaseVersionManager versionManager, Preferences prefs, BriefcaseDb db, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort) {
    return new Workspace(http, versionManager, prefs, db, formMetadataPort, submissionMetadataPort);
  }

  public Path get() {
    return workspaceLocation.orElseThrow(() -> new BriefcaseException("Workspace not initialized"));
  }

  private Path getFormsDir() {
    return get().resolve("forms");
  }

  public Workspace startAt(Path workspaceLocation) {
    this.workspaceLocation = Optional.of(workspaceLocation);
    createDirectories(getFormsDir());
    saveLocation(workspaceLocation);
    db.startAt(workspaceLocation);
    Flyway.configure().locations("db/migration")
        .dataSource(db.getDsn(), db.getUser(), db.getPassword()).validateOnMigrate(false)
        .load().migrate();
    return this;
  }

  public Path buildFormFile(FormMetadata formMetadata) {
    String baseName = stripIllegalChars(formMetadata.getFormName().orElse(formMetadata.getKey().getId()));
    return getFormsDir().resolve(baseName).resolve(baseName + ".xml");
  }

  public void stop() {
    db.stop();
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

  public Workspace withWorkspaceLocation(Path workspaceLocation) {
    this.workspaceLocation = Optional.of(workspaceLocation);
    return this;
  }
}
