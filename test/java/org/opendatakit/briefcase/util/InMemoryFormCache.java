package org.opendatakit.briefcase.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public class InMemoryFormCache implements FormCacheable {
  private final Map<String, String> hashesByFormName = new HashMap<>();
  private final Map<String, BriefcaseFormDefinition> formsByName = new HashMap<>();

  @Override
  public String getFormFileMd5Hash(String filePath) {
    return hashesByFormName.get(filePath);
  }

  @Override
  public void putFormFileMd5Hash(String filePath, String md5Hash) {
    hashesByFormName.put(filePath, md5Hash);
  }

  @Override
  public BriefcaseFormDefinition getFormFileFormDefinition(String filePath) {
    return formsByName.get(filePath);
  }

  @Override
  public void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition) {
    formsByName.put(filePath, definition);
  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return new ArrayList<>(formsByName.values());
  }

  @Override
  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return formsByName.values().stream()
        .filter(formDefinition -> formDefinition.getFormName().equals(formName)).findFirst();
  }
}
