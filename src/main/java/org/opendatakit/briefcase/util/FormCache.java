package org.opendatakit.briefcase.util;

import static org.opendatakit.briefcase.reused.Predicates.negate;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;

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
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormCache {
  private static final Logger log = LoggerFactory.getLogger(FormCache.class);
  private static final String CACHE_FILE_NAME = "cache.ser";
  private Optional<Path> briefcaseDir;
  private Map<String, String> hashByPath;
  private Map<String, FormStatus> formDefByPath;

  private FormCache(Map<String, String> hashByPath, Map<String, FormStatus> formDefByPath, Optional<Path> briefcaseDir) {
    this.briefcaseDir = briefcaseDir;
    this.hashByPath = hashByPath;
    this.formDefByPath = formDefByPath;
    update();
    AnnotationProcessor.process(this);
  }

  public static FormCache empty() {
    return new FormCache(new HashMap<>(), new HashMap<>(), Optional.empty());
  }

  public static FormCache from(Path briefcaseDir) {
    FormCache formCache = empty();
    formCache.setLocation(briefcaseDir);
    return formCache;
  }

  public void setLocation(Path newBriefcaseDir) {
    briefcaseDir = Optional.of(newBriefcaseDir);
  }

  public void unsetLocation() {
    briefcaseDir = Optional.empty();
    hashByPath = new HashMap<>();
    formDefByPath = new HashMap<>();
  }

  public List<FormStatus> getForms() {
    return new ArrayList<>(formDefByPath.values());
  }

  public void update() {
    briefcaseDir.ifPresent(path -> {
      Set<String> scannedFiles = new HashSet<>();
      list(path.resolve("forms"))
          .filter(UncheckedFiles::isFormDir)
          .forEach(formDir -> {
            Path form = getFormFilePath(formDir);
            scannedFiles.add(form.toString());
            String hash = FileSystemUtils.getMd5Hash(form.toFile());
            if (isFormNewOrChanged(form, hash)) {
              formDefByPath.put(form.toString(), new FormStatus(FormMetadata.from(form)));
              hashByPath.put(form.toString(), hash);
            }
          });
      // Warning: Remove map entries by mutating the key set works because the key set is a view on the map
      hashByPath.keySet().removeIf(negate(scannedFiles::contains));
      formDefByPath.keySet().removeIf(negate(scannedFiles::contains));
      EventBus.publish(new CacheUpdateEvent());
    });
  }

  private boolean isFormNewOrChanged(Path form, String hash) {
    return hashByPath.get(form.toString()) == null
        || formDefByPath.get(form.toString()) == null
        || !hashByPath.get(form.toString()).equalsIgnoreCase(hash);
  }

  private Path getFormFilePath(Path formDir) {
    return formDir.resolve(formDir.getFileName().toString() + ".xml");
  }

  @EventSubscriber(eventClass = PullEvent.Success.class)
  public void onPullSuccess(PullEvent.Success event) {
    update();
  }

  @EventSubscriber(eventClass = PullEvent.Cancel.class)
  public void onPullCancel(PullEvent.Cancel event) {
    update();
  }
}
