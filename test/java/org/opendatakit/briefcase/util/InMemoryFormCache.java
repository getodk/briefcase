package org.opendatakit.briefcase.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public class InMemoryFormCache implements FormCacheable {
  private final Map<String, String> hashes = new HashMap<>();
  private final Map<String, BriefcaseFormDefinition> forms = new HashMap<>();

  @Override
  public String getFormFileMd5Hash(String filePath) {
    return hashes.get(filePath);
  }

  @Override
  public void putFormFileMd5Hash(String filePath, String md5Hash) {
    hashes.put(filePath, md5Hash);
  }

  @Override
  public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
    return forms.get(filePath);
  }

  @Override
  public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
    forms.put(filePath, definition);
  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return new ArrayList<>(forms.values());
  }
}
