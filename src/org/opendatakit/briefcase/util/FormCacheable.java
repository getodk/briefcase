package org.opendatakit.briefcase.util;

import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

public interface FormCacheable {
  List<BriefcaseFormDefinition> getForms();

  Optional<BriefcaseFormDefinition> getForm(String formName);

  void update();
}
