package org.opendatakit.briefcase.util;

import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;
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
      } catch (IOException | ClassNotFoundException e) {
        // We can't read the forms cache file for some reason. Log it, delete it,
        // and let the next block create it new.
        log.warn("Can't read forms cache file", e);
        UncheckedFiles.delete(cacheFile);
      }
    UncheckedFiles.createFile(cacheFile);
    return new FormCache(cacheFile, new HashMap<>(), new HashMap<>());
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
}
