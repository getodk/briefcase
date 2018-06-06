package org.opendatakit.briefcase.util;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;

/**
 * Until the storage location is set, there is no place for the cache file. This class allows avoiding null checks.
 */
public class NullFormCache implements FormCacheable {

  @Override
  public void setLocation(Path newBriefcaseDir) {
    // Do nothing
  }

  @Override
  public void unsetLocation() {

  }

  @Override
  public List<BriefcaseFormDefinition> getForms() {
    return Collections.emptyList();
  }

  @Override
  public Optional<BriefcaseFormDefinition> getForm(String formName) {
    return Optional.empty();
  }

  @Override
  public void update() {
    // Do nothing
  }
}
