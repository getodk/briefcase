package org.opendatakit.briefcase.reused;

import static org.opendatakit.briefcase.reused.db.StartType.NEW_DB;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getHttpProxy;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getMaxHttpConnections;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getTrackingConsent;

import io.sentry.SentryClient;
import java.nio.file.Path;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.db.StartType;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.form.FormMetadataCommands;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.preferences.PreferencePort;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataCommands;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;

public class Container {
  public final Workspace workspace;
  public final Http http;
  public final BriefcaseVersionManager versionManager;
  public final BriefcaseDb db;
  public final SentryClient sentry;
  public final FormMetadataPort formMetadata;
  public final SubmissionMetadataPort submissionMetadata;
  public final PreferencePort preferences;

  public Container(Workspace workspace, Http http, BriefcaseVersionManager versionManager, BriefcaseDb db, SentryClient sentry, FormMetadataPort formMetadata, SubmissionMetadataPort submissionMetadata, PreferencePort preferences) {
    this.workspace = workspace;
    this.http = http;
    this.versionManager = versionManager;
    this.db = db;
    this.sentry = sentry;
    this.formMetadata = formMetadata;
    this.submissionMetadata = submissionMetadata;
    this.preferences = preferences;
  }

  public void start(Path workspaceLocation, Optional<Integer> maybeMaxHttpConnections) {
    // First, set the workspace and start the db
    workspace.setWorkspaceLocation(workspaceLocation);
    StartType startType = db.startAt(workspaceLocation);

    // Second, run migrations
    Flyway.configure().locations("db/migration")
        .dataSource(db.getDsn(), db.getUser(), db.getPassword()).validateOnMigrate(false)
        .load().migrate();

    // Third, set anything related to prefs
    Optionals.race(maybeMaxHttpConnections, preferences.query(getMaxHttpConnections())).ifPresent(http::setMaxHttpConnections);
    preferences.query(getHttpProxy()).ifPresent(http::setProxy);
    sentry.addShouldSendEventCallback(event -> preferences.query(getTrackingConsent()));

    // Fourth, if we're starting a new db, then sync form & submission metadata
    if (startType == NEW_DB) {
      formMetadata.execute(FormMetadataCommands.syncWithFilesAt(workspaceLocation));
      submissionMetadata.execute(SubmissionMetadataCommands.syncSubmissions(formMetadata.fetchAll()));
    }
  }

  public void stop() {
    db.stop();
  }
}
