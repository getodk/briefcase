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

import static java.lang.Boolean.TRUE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.opendatakit.briefcase.ui.BriefcaseCLI.launchLegacyCLI;
import static org.opendatakit.briefcase.ui.MessageStrings.BRIEFCASE_WELCOME;
import static org.opendatakit.briefcase.ui.MessageStrings.TRACKING_WARNING;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.buildconfig.BuildConfig;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.StorageLocationEvent;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.ui.pull.PullPanel;
import org.opendatakit.briefcase.ui.push.PushPanel;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.settings.SettingsPanel;
import org.opendatakit.briefcase.util.FormCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBriefcaseWindow extends WindowAdapter {
  private static final Logger log = LoggerFactory.getLogger(BaseFormParserForJavaRosa.class.getName());
  private static final String APP_NAME = "ODK Briefcase";
  private static final String BRIEFCASE_VERSION = APP_NAME + " - " + BuildConfig.VERSION;
  private static final String TRACKING_WARNING_SHOWED_PREF_KEY = "tracking warning showed";

  private final JFrame frame;
  private final TerminationFuture transferTerminationFuture = new TerminationFuture();
  private final JTabbedPane tabbedPane;
  private final Map<String, Integer> tabTitleIndexes = new HashMap<>();

  public static void main(String[] args) {
    if (args.length == 0)
      launchGUI();
    else
      launchLegacyCLI(args);
  }

  public static void launchGUI() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      new MainBriefcaseWindow();
    } catch (Exception e) {
      log.error("Failed to launch GUI", e);
      System.err.println("Failed to launch Briefcase GUI");
      System.exit(1);
    }
  }

  private MainBriefcaseWindow() {
    AnnotationProcessor.process(this);

    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    FormCache formCache = appPreferences.getBriefcaseDir()
        .map(FormCache::from)
        .orElse(FormCache.empty());

    frame = new JFrame();

    Analytics analytics = Analytics.from(
        BuildConfig.GOOGLE_TRACKING_ID,
        BuildConfig.VERSION,
        BriefcasePreferences.getUniqueUserID(),
        Toolkit.getDefaultToolkit().getScreenSize(),
        frame::getSize
    );
    analytics.enableTracking(BriefcasePreferences.getBriefcaseTrackingConsentProperty());
    analytics.enter("Briefcase");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> analytics.leave("Briefcase")));

    Http http = new CommonsHttp();

    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    frame.setContentPane(tabbedPane);

    addPane(PullPanel.TAB_NAME, PullPanel.from(http, appPreferences, transferTerminationFuture).getContainer());

    PushPanel pushPanel = PushPanel.from(http, appPreferences, transferTerminationFuture, formCache);
    addPane(PushPanel.TAB_NAME, pushPanel.getContainer());

    ExportPanel exportPanel = ExportPanel.from(BriefcasePreferences.forClass(ExportPanel.class), appPreferences, analytics, formCache);
    addPane(ExportPanel.TAB_NAME, exportPanel.getForm().getContainer());

    Component settingsPanel = SettingsPanel.from(appPreferences, analytics, formCache).getContainer();
    addPane(SettingsPanel.TAB_NAME, settingsPanel);

    frame.addWindowListener(this);
    frame.setTitle(BRIEFCASE_VERSION);
    ImageIcon imageIcon = new ImageIcon(getClass().getClassLoader().getResource("odk_logo.png"));
    frame.setIconImage(imageIcon.getImage());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    if (isFirstLaunch(appPreferences)) {
      lockUI();
      showWelcomeMessage();
      appPreferences.put(TRACKING_WARNING_SHOWED_PREF_KEY, TRUE.toString());
    }

    // Starting with Briefcase version 1.10.0, tracking is enabled by default.
    // Users upgrading from previous versions must be warned about this.
    if (isFirstLaunchAfterTrackingUpgrade(appPreferences)) {
      showTrackingWarning();
      appPreferences.put(TRACKING_WARNING_SHOWED_PREF_KEY, TRUE.toString());
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
    showMessageDialog(frame, TRACKING_WARNING, APP_NAME, PLAIN_MESSAGE);
  }

  private void showWelcomeMessage() {
    showMessageDialog(frame, BRIEFCASE_WELCOME, APP_NAME, PLAIN_MESSAGE);
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

  @Override
  public void windowClosing(WindowEvent arg0) {
    transferTerminationFuture.markAsCancelled(new PullEvent.Abort("Main window closed"));
    transferTerminationFuture.markAsCancelled(new PushEvent.Abort("Main window closed"));
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
