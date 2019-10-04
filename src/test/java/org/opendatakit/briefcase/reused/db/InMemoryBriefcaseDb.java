package org.opendatakit.briefcase.reused.db;

import java.nio.file.Path;

public class InMemoryBriefcaseDb extends BriefcaseDb {

  public InMemoryBriefcaseDb() {
    super(null);
  }

  @Override
  public StartType startAt(Path workspaceLocation) {
    return StartType.EXISTING_DB;
  }

  @Override
  public void stop() {

  }
}
