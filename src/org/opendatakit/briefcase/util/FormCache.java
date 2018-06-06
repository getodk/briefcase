package org.opendatakit.briefcase.util;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createFile;
import static org.opendatakit.briefcase.reused.UncheckedFiles.delete;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
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
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormCache implements FormCacheable {
  private static final Logger log = LoggerFactory.getLogger(FormCache.class);
  private static final String CACHE_FILE_NAME = "cache.ser";
  private final Path cacheFile;
  private final Map<String, String> hashByPath;
  private final Map<String, BriefcaseFormDefinition> formDefByPath;

  public FormCache(Path cacheFile, Map<String, String> hashByPath, Map<String, BriefcaseFormDefinition> formDefByPath) {
    this.cacheFile = cacheFile;
    this.hashByPath = hashByPath;
    this.formDefByPath = formDefByPath;
    Runtime.getRuntime().addShutdownHook(new Thread(this::save));
  }

  @SuppressWarnings("unchecked")
  public static FormCache from(Path briefcaseDir) {
    Path cacheFile = briefcaseDir.resolve(CACHE_FILE_NAME);
    if (exists(cacheFile))
      try (InputStream in = Files.newInputStream(cacheFile);
           ObjectInputStream ois = new ObjectInputStream(in)) {
        Map<String, String> pathToMd5Map = (Map<String, String>) ois.readObject();
        Map<String, BriefcaseFormDefinition> pathToDefinitionMap = (Map<String, BriefcaseFormDefinition>) ois.readObject();
        return new FormCache(cacheFile, pathToMd5Map, pathToDefinitionMap);
      } catch (InvalidClassException e) {
        log.warn("The serialized forms cache is incompatible due to an update on Briefcase");
        delete(cacheFile);
      } catch (IOException | ClassNotFoundException e) {
        // We can't read the forms cache file for some reason. Log it, delete it,
        // and let the next block create it new.
        log.warn("Can't read forms cache file", e);
        delete(cacheFile);
      }
    createFile(cacheFile);
    FormCache formCache = new FormCache(cacheFile, new HashMap<>(), new HashMap<>());
    formCache.update(briefcaseDir);
    return formCache;
  }

  private void save() {
    try (OutputStream out = Files.newOutputStream(cacheFile);
         ObjectOutputStream oos = new ObjectOutputStream(out)) {
      oos.writeObject(hashByPath);
      oos.writeObject(formDefByPath);
    } catch (IOException e) {
      log.error("Can't serialize form cache", e);
    }
  }

  @Override
  public String getFormFileMd5Hash(String filePath) {
    return hashByPath.get(filePath);
  }

  @Override
  public void putFormFileMd5Hash(String filePath, String md5Hash) {
    hashByPath.put(filePath, md5Hash);
  }

  @Override
  public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
    return formDefByPath.get(filePath);
  }

  @Override
  public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
    formDefByPath.put(filePath, definition);
  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return new ArrayList<>(formDefByPath.values());
  }

  @Override
  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return formDefByPath.values().stream()
        .filter(formDefinition -> formDefinition.getFormName().equals(formName))
        .findFirst();
  }

  @Override
  public void update(Path briefcaseDir) {
    Set<String> scannedFiles = new HashSet<>();
    list(briefcaseDir.resolve("forms"))
        .filter(path -> Files.isDirectory(path) && Files.exists(getForm(path)))
        .map(this::getForm)
        .peek(path -> scannedFiles.add(path.toString()))
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
    hashByPath.keySet().stream().filter(path -> !scannedFiles.contains(path)).collect(toList()).forEach(hashByPath::remove);
    formDefByPath.keySet().stream().filter(path -> !scannedFiles.contains(path)).collect(toList()).forEach(formDefByPath::remove);
    EventBus.publish(new CacheUpdateEvent());
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
}
