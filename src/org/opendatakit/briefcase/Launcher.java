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

import static java.lang.Boolean.TRUE;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_DSN;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_ENABLED;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.model.BriefcasePreferences.BRIEFCASE_TRACKING_CONSENT_PROPERTY;
import static org.opendatakit.briefcase.operations.ClearPreferences.CLEAR_PREFS;
import static org.opendatakit.briefcase.operations.Export.EXPORT_FORM;
import static org.opendatakit.briefcase.operations.ImportFromODK.IMPORT_FROM_ODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.DEPRECATED_PULL_AGGREGATE;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.PULL_FORM_FROM_AGGREGATE;
import static org.opendatakit.briefcase.operations.PushFormToAggregate.PUSH_FORM_TO_AGGREGATE;
import static org.opendatakit.briefcase.ui.BriefcaseCLI.runLegacyCli;
import static org.opendatakit.briefcase.ui.MainBriefcaseWindow.launchGUI;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.getOsName;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.common.cli.Cli;
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

  public static void main(String[] args) {
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    if (!appPreferences.hasKey(BRIEFCASE_TRACKING_CONSENT_PROPERTY))
      appPreferences.put(BRIEFCASE_TRACKING_CONSENT_PROPERTY, TRUE.toString());

    Optional<SentryClient> sentry = SENTRY_ENABLED ? Optional.of(initSentryClient(appPreferences)) : Optional.empty();

    new Cli()
        .deprecate(DEPRECATED_PULL_AGGREGATE, PULL_FORM_FROM_AGGREGATE)
        .register(PULL_FORM_FROM_AGGREGATE)
        .register(PUSH_FORM_TO_AGGREGATE)
        .register(IMPORT_FROM_ODK)
        .register(EXPORT_FORM)
        .register(CLEAR_PREFS)
        .otherwise((cli, commandLine) -> {
          if (args.length == 0)
            launchGUI();
          else
            runLegacyCli(commandLine, cli::printHelp);
        })
        .onError(throwable -> {
          System.err.println(throwable instanceof BriefcaseException
              ? "Error: " + throwable.getMessage()
              : "Unexpected error in Briefcase. Please review briefcase.log for more information. For help, post to https://forum.opendatakit.org/c/support");
          log.error("Error", throwable);
          sentry.ifPresent(client -> client.sendException(throwable));
          System.exit(1);
        })
        .run(args);
  }

  private static SentryClient initSentryClient(BriefcasePreferences appPreferences) {
    Sentry.init(String.format(
        "%s?release=%s&stacktrace.app.packages=org.opendatakit&tags=os:%s,jvm:%s",
        SENTRY_DSN,
        VERSION,
        getOsName(),
        System.getProperty("java.version")
    ));

    SentryClient sentry = Sentry.getStoredClient();

    // Add a callback that will prevent sending crash reports to Sentry
    // if the user disables tracking
    sentry.addShouldSendEventCallback(event -> appPreferences
        .nullSafeGet(BRIEFCASE_TRACKING_CONSENT_PROPERTY)
        .map(Boolean::valueOf)
        .orElse(true));

    return sentry;
  }
}
