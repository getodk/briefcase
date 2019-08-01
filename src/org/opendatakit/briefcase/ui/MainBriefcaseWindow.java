/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui;

import static java.awt.GridBagConstraints.WEST;
import static java.lang.Runtime.getRuntime;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.ui.BriefcaseCLI.launchLegacyCLI;
import static org.opendatakit.briefcase.ui.MessageStrings.BRIEFCASE_WELCOME;
import static org.opendatakit.briefcase.ui.MessageStrings.TRACKING_WARNING;
import static org.opendatakit.briefcase.ui.reused.UI.infoMessage;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.buildconfig.BuildConfig;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.StorageLocationEvent;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.ui.pull.PullPanel;
import org.opendatakit.briefcase.ui.push.PushPanel;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.settings.SettingsPanel;
import org.opendatakit.briefcase.util.BriefcaseVersionManager;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.briefcase.util.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBriefcaseWindow extends WindowAdapter {
  private static final Logger log = LoggerFactory.getLogger(MainBriefcaseWindow.class.getName());
  public static final String APP_NAME = "ODK Briefcase";

  private final JFrame frame = new JFrame();
  private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
  private final Map<String, Integer> tabTitleIndexes = new HashMap<>();

  public static void main(String[] args) {
    if (args.length == 0)
      launchGUI();
    else
      launchLegacyCLI(args);
  }

  public static void launchGUI() {
    try {
      if (Host.isLinux())
        UIManager.setLookAndFeel(new MetalLookAndFeel());
      else
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      new MainBriefcaseWindow();
    } catch (Exception e) {
      log.error("Failed to launch GUI", e);
      System.err.println("Failed to launch Briefcase GUI");
      System.exit(1);
    }
  }

  private MainBriefcaseWindow() {
    // Create all dependencies
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    BriefcasePreferences pullPreferences = BriefcasePreferences.forClass(PullPanel.class);
    BriefcasePreferences exportPreferences = BriefcasePreferences.forClass(ExportPanel.class);
    Optional<Path> briefcaseDir = appPreferences.getBriefcaseDir().filter(Files::exists);
    if (!briefcaseDir.isPresent())
      appPreferences.unsetStorageDir();
    FormCache formCache = briefcaseDir
        .map(FormCache::from)
        .orElse(FormCache.empty());

    int maxHttpConnections = appPreferences.getMaxHttpConnections().orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    BriefcaseVersionManager versionManager = new BriefcaseVersionManager(http, VERSION);

    Analytics analytics = Analytics.from(
        BuildConfig.GOOGLE_TRACKING_ID,
        VERSION,
        BriefcasePreferences.getUniqueUserID(),
        Toolkit.getDefaultToolkit().getScreenSize(),
        frame::getSize
    );
    analytics.enableTracking(BriefcasePreferences.getBriefcaseTrackingConsentProperty());
    analytics.enter("Briefcase");
    getRuntime().addShutdownHook(new Thread(() -> analytics.leave("Briefcase")));

    // Add panes to the tabbedPane
    addPane(PullPanel.TAB_NAME, PullPanel.from(http, appPreferences, pullPreferences, analytics).getContainer());
    addPane(PushPanel.TAB_NAME, PushPanel.from(http, appPreferences, formCache, analytics).getContainer());
    addPane(ExportPanel.TAB_NAME, ExportPanel.from(exportPreferences, appPreferences, pullPreferences, analytics, formCache, http).getForm().getContainer());
    addPane(SettingsPanel.TAB_NAME, SettingsPanel.from(appPreferences, analytics, formCache, http, versionManager).getContainer());

    // Set up the frame and put the UI components in it
    frame.addWindowListener(this);
    frame.setLayout(new GridBagLayout());
    frame.setTitle(APP_NAME);
    frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("odk_logo.png")).getImage());
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    GridBagConstraints tabbedPaneConstraint;
    tabbedPaneConstraint = new GridBagConstraints();
    tabbedPaneConstraint.gridx = 0;
    tabbedPaneConstraint.gridy = 0;
    tabbedPaneConstraint.weightx = 1.0;
    tabbedPaneConstraint.weighty = 1.0;
    tabbedPaneConstraint.anchor = WEST;
    frame.add(tabbedPane, tabbedPaneConstraint);
    frame.pack();

    AnnotationProcessor.process(this);

    if (isFirstLaunch(appPreferences)) {
      lockUI();
      showWelcomeMessage();
      appPreferences.setTrackingWarningShowed();
    }

    // Starting with Briefcase version 1.10.0, tracking is enabled by default.
    // Users upgrading from previous versions must be warned about this.
    if (isFirstLaunchAfterTrackingUpgrade(appPreferences)) {
      showTrackingWarning();
      appPreferences.setTrackingWarningShowed();
    }
  }

  private void lockUI() {
    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      tabbedPane.setEnabledAt(i, false);
    tabbedPane.setEnabledAt(tabTitleIndexes.get(SettingsPanel.TAB_NAME), true);
    tabbedPane.setSelectedIndex(tabTitleIndexes.get(SettingsPanel.TAB_NAME));
  }

  private void unlockUI() {
    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      tabbedPane.setEnabledAt(i, true);
  }

  private void showTrackingWarning() {
    infoMessage(TRACKING_WARNING);
  }

  private void showWelcomeMessage() {
    infoMessage(BRIEFCASE_WELCOME);
  }

  private boolean isFirstLaunchAfterTrackingUpgrade(BriefcasePreferences appPreferences) {
    return !appPreferences.hasTrackingWarningBeenShowed();
  }

  private boolean isFirstLaunch(BriefcasePreferences appPreferences) {
    return !appPreferences.getBriefcaseDir().isPresent();
  }

  private void addPane(String title, Component pane) {
    tabTitleIndexes.put(title, tabbedPane.getTabCount());
    tabbedPane.addTab(title, null, pane, null);
  }

  @EventSubscriber(eventClass = StorageLocationEvent.LocationDefined.class)
  public void onFormStatusEvent(StorageLocationEvent.LocationDefined event) {
    unlockUI();
  }

  @EventSubscriber(eventClass = StorageLocationEvent.LocationCleared.class)
  public void onFormStatusEvent(StorageLocationEvent.LocationCleared event) {
    lockUI();
  }
}
