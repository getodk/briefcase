package org.opendatakit.briefcase.util;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;

import java.io.File;
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
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormCache implements FormCacheable {
  private static final Logger log = LoggerFactory.getLogger(FormCache.class);
  private static final String CACHE_FILE_NAME = "cache.ser";
  private final Path cacheFile;
  private final Map<String, String> pathToMd5Map;
  private final Map<String, BriefcaseFormDefinition> pathToDefinitionMap;

  public FormCache(Path cacheFile, Map<String, String> pathToMd5Map, Map<String, BriefcaseFormDefinition> pathToDefinitionMap) {
    this.cacheFile = cacheFile;
    this.pathToMd5Map = pathToMd5Map;
    this.pathToDefinitionMap = pathToDefinitionMap;
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
        UncheckedFiles.delete(cacheFile);
      } catch (IOException | ClassNotFoundException e) {
        // We can't read the forms cache file for some reason. Log it, delete it,
        // and let the next block create it new.
        log.warn("Can't read forms cache file", e);
        UncheckedFiles.delete(cacheFile);
      }
    UncheckedFiles.createFile(cacheFile);
    FormCache formCache = new FormCache(cacheFile, new HashMap<>(), new HashMap<>());
    formCache.update(briefcaseDir);
    return formCache;
  }

  private void save() {
    try (OutputStream out = Files.newOutputStream(cacheFile);
         ObjectOutputStream oos = new ObjectOutputStream(out)) {
      oos.writeObject(pathToMd5Map);
      oos.writeObject(pathToDefinitionMap);
    } catch (IOException e) {
      throw new BriefcaseException("Can't serialize form cache", e);
    }
  }

  @Override
  public String getFormFileMd5Hash(String filePath) {
    return pathToMd5Map.get(filePath);
  }

  @Override
  public void putFormFileMd5Hash(String filePath, String md5Hash) {
    pathToMd5Map.put(filePath, md5Hash);
  }

  @Override
  public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
    return pathToDefinitionMap.get(filePath);
  }

  @Override
  public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
    pathToDefinitionMap.put(filePath, definition);
  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return new ArrayList<>(pathToDefinitionMap.values());
  }

  @Override
  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return pathToDefinitionMap.values().stream()
        .filter(formDefinition -> formDefinition.getFormName().equals(formName))
        .findFirst();
  }

  @Override
  public void update(Path briefcaseDir) {
    Set<String> scannedFiles = new HashSet<>();
    File forms = briefcaseDir.resolve("forms").toFile();
    if (forms.exists()) {
      File[] formDirs = forms.listFiles();
      for (File f : formDirs) {
        if (f.isDirectory()) {
          try {
            File formFile = new File(f, f.getName() + ".xml");
            String formFileHash = FileSystemUtils.getMd5Hash(formFile);
            String existingFormFileHash = getFormFileMd5Hash(formFile.getAbsolutePath());
            BriefcaseFormDefinition existingDefinition = getFormFileFormDefinition(formFile.getAbsolutePath());
            if (existingFormFileHash == null
                || existingDefinition == null
                || !existingFormFileHash.equalsIgnoreCase(formFileHash)) {
              // overwrite cache if the form's hash is not the same or there's no entry for the form in the cache.
              putFormFileMd5Hash(formFile.getAbsolutePath(), formFileHash);
              existingDefinition = new BriefcaseFormDefinition(f, formFile);
              putFormFileFormDefinition(formFile.getAbsolutePath(), existingDefinition);
            }
            scannedFiles.add(formFile.getAbsolutePath());
          } catch (BadFormDefinition e) {
            log.debug("bad form definition", e);
          }
        } else {
          // junk?
          f.delete();
        }
      }
    }
    pathToMd5Map.keySet().stream().filter(path -> !scannedFiles.contains(path)).collect(toList()).forEach(pathToMd5Map::remove);
    pathToDefinitionMap.keySet().stream().filter(path -> !scannedFiles.contains(path)).collect(toList()).forEach(pathToDefinitionMap::remove);
    EventBus.publish(new CacheUpdateEvent());
  }
}
