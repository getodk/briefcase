package org.opendatakit.briefcase.util;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createFile;
import static org.opendatakit.briefcase.reused.UncheckedFiles.delete;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormCache {
  private static final Logger log = LoggerFactory.getLogger(FormCache.class);
  private static final String CACHE_FILE_NAME = "cache.ser";
  private Optional<Path> cacheFile;
  private Optional<Path> briefcaseDir;
  private Map<String, String> hashByPath;
  private Map<String, BriefcaseFormDefinition> formDefByPath;

  private FormCache(Optional<Path> cacheFile, Map<String, String> hashByPath, Map<String, BriefcaseFormDefinition> formDefByPath) {
    this.cacheFile = cacheFile;
    this.briefcaseDir = cacheFile.map(Path::getParent);
    this.hashByPath = hashByPath;
    this.formDefByPath = formDefByPath;
    AnnotationProcessor.process(this);
  }

  public static FormCache empty() {
    return new FormCache(Optional.empty(), new HashMap<>(), new HashMap<>());
  }

  public static FormCache from(Path briefcaseDir) {
    FormCache formCache = empty();
    formCache.setLocation(briefcaseDir);
    return formCache;
  }

  @SuppressWarnings("unchecked")
  public void setLocation(Path newBriefcaseDir) {
    briefcaseDir = Optional.of(newBriefcaseDir);
    Path cacheFilePath = newBriefcaseDir.resolve(CACHE_FILE_NAME);
    cacheFile = Optional.of(cacheFilePath);
    if (Files.exists(cacheFilePath))
      try (InputStream in = Files.newInputStream(cacheFilePath);
           ObjectInputStream ois = new ObjectInputStream(in)) {
        hashByPath = (Map<String, String>) ois.readObject();
        formDefByPath = (Map<String, BriefcaseFormDefinition>) ois.readObject();
      } catch (InvalidClassException e) {
        log.warn("The serialized forms cache is incompatible due to an update on Briefcase");
        delete(cacheFilePath);
      } catch (IOException | ClassNotFoundException e) {
        // We can't read the forms cache file for some reason. Log it, delete it,
        // and let the next block create it new.
        log.warn("Can't read forms cache file", e);
        delete(cacheFilePath);
      }

    // Check again since it could be deleted in the previous block
    if (!Files.exists(cacheFilePath)) {
      createFile(cacheFilePath);
      hashByPath = new HashMap<>();
      formDefByPath = new HashMap<>();
    }
    update();
  }

  public void unsetLocation() {
    briefcaseDir = Optional.empty();
    cacheFile = Optional.empty();
    hashByPath = new HashMap<>();
    formDefByPath = new HashMap<>();
    update();
  }

  private void save() {
    cacheFile.ifPresent(path -> {
      try (OutputStream out = Files.newOutputStream(path);
           ObjectOutputStream oos = new ObjectOutputStream(out)) {
        oos.writeObject(hashByPath);
        oos.writeObject(formDefByPath);
      } catch (IOException e) {
        log.error("Can't serialize form cache", e);
      }
    });
  }

  public List<BriefcaseFormDefinition> getForms() {
    return new ArrayList<>(formDefByPath.values());
  }

  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return formDefByPath.values().stream()
        .filter(formDefinition -> formDefinition.getFormName().equals(formName))
        .findFirst();
  }

  public void update() {
    briefcaseDir.ifPresent(path -> {
      Set<String> scannedFiles = new HashSet<>();
      list(path.resolve("forms"))
          .map(this::getForm)
          .peek(p -> scannedFiles.add(p.toString()))
          .forEach(form -> {
            String hash = FileSystemUtils.getMd5Hash(form.toFile());
            if (isFormNewOrChanged(form, hash)) {
              try {
                formDefByPath.put(form.toString(), new BriefcaseFormDefinition(form.getParent().toFile(), form.toFile()));
                hashByPath.put(form.toString(), hash);
              } catch (BadFormDefinition e) {
                log.warn("Can't parse form file", e);
              }
            }
          });
      hashByPath.keySet().stream().filter(p -> !scannedFiles.contains(p)).collect(toList()).forEach(hashByPath::remove);
      formDefByPath.keySet().stream().filter(p -> !scannedFiles.contains(p)).collect(toList()).forEach(formDefByPath::remove);
      EventBus.publish(new CacheUpdateEvent());
      save();
    });
  }

  private boolean isFormNewOrChanged(Path form, String hash) {
    return hashByPath.get(form.toString()) == null
        || formDefByPath.get(form.toString()) == null
        || !hashByPath.get(form.toString()).equalsIgnoreCase(hash);
  }

  public Path getForm(Path path) {
    String formName = path.getFileName().toString();
    return path.resolve(formName + ".xml");
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {
    update();
  }
}
