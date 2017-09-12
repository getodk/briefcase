package org.opendatakit.briefcase.util;

import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

/** Until the storage location is set, there is no place for the cache file. This class allows avoiding null checks. */
public class NullFormCache extends FormCache {
    public NullFormCache() {
        super(null);
    }

    @Override
    public String getFormFileMd5Hash(String filePath) {
        throw new UnsupportedOperationException("getFormFileMd5Hash");
    }

    @Override
    public void putFormFileMd5Hash(String filePath, String md5Hash) {
        throw new UnsupportedOperationException("putFormFileMd5Hash");
    }

    @Override
    public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
        throw new UnsupportedOperationException("getFormFileFormDefinition");
    }

    @Override
    public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
        throw new UnsupportedOperationException("putFormFileFormDefinition");
    }
}
