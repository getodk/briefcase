package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public class FormCache implements FormCacheable {
    private final File cacheFile;
    private Map<String, String> pathToMd5Map = new HashMap<>();
    private Map<String, BriefcaseFormDefinition> pathToDefinitionMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public FormCache(File storagePath) {
        cacheFile = new File(storagePath, "cache.ser");
        if (cacheFile.exists() && cacheFile.canRead()) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(cacheFile))) {
                pathToMd5Map = (Map) objectInputStream.readObject();
                pathToDefinitionMap = (Map) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                save();
            }
        });
    }

    private void save() {
        if (!cacheFile.exists() || cacheFile.canWrite()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                objectOutputStream.writeObject(pathToMd5Map);
                objectOutputStream.writeObject(pathToDefinitionMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        if (pathToDefinitionMap == null) {
            pathToDefinitionMap = new HashMap<>();
        }
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
