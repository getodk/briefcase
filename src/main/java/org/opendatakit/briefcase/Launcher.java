/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase;

import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_DSN;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_ENABLED;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.delivery.cli.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.delivery.cli.Common.WORKSPACE_LOCATION;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.model.Host.getOsName;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getLegacyPrefsStatus;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import java.security.Security;
import java.util.Optional;
import java.util.prefs.Preferences;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendatakit.briefcase.delivery.LegacyPrefs;
import org.opendatakit.briefcase.delivery.cli.Export;
import org.opendatakit.briefcase.delivery.cli.PullFromAggregate;
import org.opendatakit.briefcase.delivery.cli.PullFromCentral;
import org.opendatakit.briefcase.delivery.cli.PullFromCollect;
import org.opendatakit.briefcase.delivery.cli.PushToAggregate;
import org.opendatakit.briefcase.delivery.cli.PushToCentral;
import org.opendatakit.briefcase.delivery.cli.launchgui.LaunchGui;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.BriefcaseVersionManager;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.NoOpSentryClient;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.cli.Cli;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.model.form.DatabaseFormMetadataAdapter;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.preferences.DatabasePreferenceAdapter;
import org.opendatakit.briefcase.reused.model.preferences.PreferencePort;
import org.opendatakit.briefcase.reused.model.submission.DatabaseSubmissionMetadataAdapter;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main launcher for Briefcase
 * <p>
 * It leverages the command-line {@link Cli} adapter to define operations and run
 * Briefcase with some command-line args
 */
public class Launcher {
  private static final Logger log = LoggerFactory.getLogger(Launcher.class);

  public static void main(String[] rawArgs) {
    Security.addProvider(new BouncyCastleProvider());

    Workspace workspace = new Workspace(Preferences.userNodeForPackage(Workspace.class));
    Http http = CommonsHttp.of(DEFAULT_HTTP_CONNECTIONS, Optional.empty());
    BriefcaseVersionManager versionManager = new BriefcaseVersionManager(http, VERSION);
    BriefcaseDb db = BriefcaseDb.create();
    SentryClient sentry = SENTRY_ENABLED ? initSentryClient() : new NoOpSentryClient();
    FormMetadataPort formMetadataPort = DatabaseFormMetadataAdapter.from(workspace, db);
    SubmissionMetadataPort submissionMetadataPort = DatabaseSubmissionMetadataAdapter.from(workspace, db);
    PreferencePort preferencePort = DatabasePreferenceAdapter.from(db);

    Container container = new Container(workspace, http, versionManager, db, sentry, formMetadataPort, submissionMetadataPort, preferencePort);

    new Cli()
        .registerDefault(LaunchGui.create(container))
        .register(PullFromAggregate.create(container))
        .register(PullFromCentral.create(container))
        .register(PullFromCollect.create(container))
        .register(PushToAggregate.create(container))
        .register(PushToCentral.create(container))
        .register(Export.create(container))
        .before((args, op) -> {
          if (!op.requiresContainer())
            return;

          // Ask for the workspace location if the user hasn't provided one with the -wl arg
          if (args.getOptional(WORKSPACE_LOCATION).isEmpty())
            op.deliveryType.promptWorkspaceLocation(container, path -> args.set(WORKSPACE_LOCATION, path.toString()));

          // Start the container
          container.start(
              args.get(WORKSPACE_LOCATION),
              args.getOptional(MAX_HTTP_CONNECTIONS)
          );

          // Run the legacy prefs workflow
          if (container.preferences.query(getLegacyPrefsStatus()).isUnresolved() && LegacyPrefs.prefsDetected())
            op.deliveryType.promptLegacyPrefsDecision(container);
        })
        .onError(throwable -> {
          System.err.println(throwable instanceof BriefcaseException
              ? "Error: " + throwable.getMessage()
              : "Unexpected error in Briefcase. Please review briefcase.log for more information. For help, post to https://forum.opendatakit.org/c/support");
          log.error("Error", throwable);
          container.sentry.sendException(throwable);
          System.exit(1);
        })
        .onExit(container::stop)
        .run(rawArgs);
  }

  private static SentryClient initSentryClient() {
    Sentry.init(String.format(
        "%s?release=%s&stacktrace.app.packages=org.opendatakit&tags=os:%s,jvm:%s",
        SENTRY_DSN,
        VERSION,
        getOsName(),
        System.getProperty("java.version")
    ));

    return Sentry.getStoredClient();
  }
}
