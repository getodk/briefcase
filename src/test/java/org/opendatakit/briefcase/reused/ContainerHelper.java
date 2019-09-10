package org.opendatakit.briefcase.reused;

import java.nio.file.Path;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.db.InMemoryBriefcaseDb;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.reused.model.preferences.InMemoryPreferences;
import org.opendatakit.briefcase.reused.model.submission.InMemorySubmissionMetadataAdapter;

public class ContainerHelper {

  public static Container inMemory() {
    Path workspaceLocation = UncheckedFiles.createTempDirectory("briefcase-workspace-");
    Workspace workspace = new Workspace(InMemoryPreferences.empty());
    workspace.setWorkspaceLocation(workspaceLocation);
    return new Container(
        workspace,
        new InMemoryHttp(),
        null,
        new InMemoryBriefcaseDb(),
        new InMemoryFormMetadataAdapter(),
        new InMemorySubmissionMetadataAdapter()
    );
  }

  public static Container inMemory(Http http) {
    Path workspaceLocation = UncheckedFiles.createTempDirectory("briefcase-workspace-");
    Workspace workspace = new Workspace(InMemoryPreferences.empty());
    workspace.setWorkspaceLocation(workspaceLocation);
    return new Container(
        workspace,
        http,
        null,
        new InMemoryBriefcaseDb(),
        new InMemoryFormMetadataAdapter(),
        new InMemorySubmissionMetadataAdapter()
    );
  }
}
