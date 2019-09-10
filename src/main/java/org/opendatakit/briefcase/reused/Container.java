package org.opendatakit.briefcase.reused;

import org.flywaydb.core.Flyway;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;

public class Container {
  public final Workspace workspace;
  public final Http http;
  public final BriefcaseVersionManager versionManager;
  public final BriefcaseDb db;
  public final FormMetadataPort formMetadata;
  public final SubmissionMetadataPort submissionMetadata;

  public Container(Workspace workspace, Http http, BriefcaseVersionManager versionManager, BriefcaseDb db, FormMetadataPort formMetadata, SubmissionMetadataPort submissionMetadata) {
    this.workspace = workspace;
    this.http = http;
    this.versionManager = versionManager;
    this.db = db;
    this.formMetadata = formMetadata;
    this.submissionMetadata = submissionMetadata;
  }

  public void start() {
    db.startAt(workspace.get());
    Flyway.configure().locations("db/migration")
        .dataSource(db.getDsn(), db.getUser(), db.getPassword()).validateOnMigrate(false)
        .load().migrate();
  }

  public void stop() {
    db.stop();
  }
}
