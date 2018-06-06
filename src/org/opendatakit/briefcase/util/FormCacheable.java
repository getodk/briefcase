package org.opendatakit.briefcase.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public interface FormCacheable {
  String getFormFileMd5Hash(String filePath);

  void putFormFileMd5Hash(String filePath, String md5Hash);

  BriefcaseFormDefinition getFormFileFormDefinition(String filePath);

  void putFormFileFormDefinition(String filePath, BriefcaseFormDefinition definition);

  List<BriefcaseFormDefinition> getForms();

  Optional<BriefcaseFormDefinition> getForm(String formName);

  void update();
}
