package org.opendatakit.briefcase.reused;

import static org.opendatakit.briefcase.reused.api.StringUtils.stripIllegalChars;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

public class Workspace {
  private final List<Consumer<Path>> startCallbacks = new ArrayList<>();
  private final List<Runnable> stopCallbacks = new ArrayList<>();
  private Optional<Path> workspaceLocation = Optional.empty();

  public static Workspace empty() {
    return new Workspace();
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

  public List<Path> getSavedLocations() {
//    return Collections.emptyList();
    return Arrays.asList(Paths.get("/tmp/cocotero"), Paths.get("/home/guillermo"));
  }
}
