package org.opendatakit.briefcase.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public interface FormCacheable {
  void setLocation(Path newBriefcaseDir);

  List<BriefcaseFormDefinition> getForms();

  Optional<BriefcaseFormDefinition> getForm(String formName);

  void update();
}
