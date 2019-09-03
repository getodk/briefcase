package org.opendatakit.briefcase.reused.db;

import java.nio.file.Path;

public class InMemoryBriefcaseDb extends BriefcaseDb {

  public InMemoryBriefcaseDb() {
    super(null);
  }

  @Override
  public void startAt(Path workspaceLocation) {

  }

  @Override
  public void stop() {

  }
}
