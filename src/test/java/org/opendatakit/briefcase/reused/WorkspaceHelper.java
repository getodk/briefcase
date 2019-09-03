package org.opendatakit.briefcase.reused;

import org.opendatakit.briefcase.reused.db.InMemoryBriefcaseDb;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.reused.model.preferences.InMemoryPreferences;
import org.opendatakit.briefcase.reused.model.submission.InMemorySubmissionMetadataAdapter;

public class WorkspaceHelper {

  public static Workspace inMemory() {
    return Workspace.with(new InMemoryHttp(), null, InMemoryPreferences.empty(), new InMemoryBriefcaseDb(), new InMemoryFormMetadataAdapter(), new InMemorySubmissionMetadataAdapter());
  }

  public static Workspace inMemory(Http http) {
    return Workspace.with(http, null, InMemoryPreferences.empty(), new InMemoryBriefcaseDb(), new InMemoryFormMetadataAdapter(), new InMemorySubmissionMetadataAdapter());
  }
}
