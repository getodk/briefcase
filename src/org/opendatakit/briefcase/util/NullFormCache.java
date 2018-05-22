package org.opendatakit.briefcase.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

/**
 * Until the storage location is set, there is no place for the cache file. This class allows avoiding null checks.
 */
public class NullFormCache implements FormCacheable {
  @Override
  public String getFormFileMd5Hash(String filePath) {
    return null;
  }

  @Override
  public void putFormFileMd5Hash(String filePath, String md5Hash) {
  }

  @Override
  public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
    return null;
  }

  @Override
  public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return Collections.emptyList();
  }

  @Override
  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return Optional.empty();
  }
}
