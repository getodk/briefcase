package org.opendatakit.briefcase.util;

import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class CacheUtils {

    static final File cacheFile = new File(FileSystemUtils.getBriefcaseFolder(), "cache.ser");
    static Map<String, String> pathToMd5Map;
    static Map<String, BriefcaseFormDefinition> pathToDefinitionMap;

    public static void saveFormDefinitionCache() {
        if (!cacheFile.exists() || cacheFile.canWrite()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                objectOutputStream.writeObject(pathToMd5Map);
                objectOutputStream.writeObject(pathToDefinitionMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void initFormDefinitionCache() {
        if (cacheFile.exists() && cacheFile.canRead()) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(cacheFile))) {
                pathToMd5Map = (Map) objectInputStream.readObject();
                pathToDefinitionMap = (Map) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                CacheUtils.saveFormDefinitionCache();
            }
        });
    }

    public static String getFormFileMd5Hash(String filePath) {
        if (pathToMd5Map == null) {
            pathToMd5Map = new HashMap<>();
        }
        return pathToMd5Map.get(filePath);
    }

    public static void putFormFileMd5Hash(String filePath, String md5Hash) {
        pathToMd5Map.put(filePath, md5Hash);
    }

    public static BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
        if (pathToDefinitionMap == null) {
            pathToDefinitionMap = new HashMap<>();
        }
        return pathToDefinitionMap.get(filePath);
    }

    public static void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
        pathToDefinitionMap.put(filePath, definition);
    }
}
