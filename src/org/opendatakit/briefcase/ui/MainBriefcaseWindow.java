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
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.buildconfig.BuildConfig;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportAbortEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.ui.pull.PullPanel;
import org.opendatakit.briefcase.ui.push.PushPanel;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.opendatakit.briefcase.ui.settings.SettingsPanel;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.FormCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBriefcaseWindow extends WindowAdapter implements UiStateChangeListener {
  private static final String APP_NAME = "ODK Briefcase";
  private static final String BRIEFCASE_VERSION = APP_NAME + " - " + BuildConfig.VERSION;
  public static final String TRACKING_WARNING_SHOWED_PREF_KEY = "tracking warning showed";
  private final ImageIcon imageIcon = new ImageIcon(getClass().getClassLoader().getResource("odk_logo.png"));

  JFrame frame;
  private PushPanel pushPanel;
  private ExportPanel exportPanel;
  private Component settingsPanel;
  private final TerminationFuture exportTerminationFuture = new TerminationFuture();
  private final TerminationFuture transferTerminationFuture = new TerminationFuture();

  private static final Logger log = LoggerFactory.getLogger(BaseFormParserForJavaRosa.class.getName());

  private final JTabbedPane tabbedPane;
  /**
   * A map from each pane to its index in the JTabbedPane
   */
  private final Map<Component, Integer> paneToIndexMap = new HashMap<>();
  private static final ExecutorService BACKGROUND_EXECUTOR = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    if (args.length == 0)
      launchGUI();
    else
      launchLegacyCLI(args);
  }

  public static void launchGUI() {
    EventQueue.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainBriefcaseWindow window = new MainBriefcaseWindow();
      } catch (Exception e) {
        log.error("failed to launch app", e);
      }
    });
  }

  @Override
  public void setFullUIEnabled(boolean enabled) {

    if (enabled) {
      exportPanel.updateForms();
      pushPanel.updateForms();
    }

    for (Map.Entry<Component, Integer> entry : paneToIndexMap.entrySet()) {
      final Component pane = entry.getKey();
      final int paneIndex = entry.getValue();
      if (pane != settingsPanel) {
        pane.setEnabled(enabled);
        tabbedPane.setEnabledAt(paneIndex, enabled);
      }
    }
    if (!enabled) {
      tabbedPane.setSelectedComponent(settingsPanel);
    }
    tabbedPane.setEnabled(enabled);
  }

  /**
   * Create the application.
   */
  private MainBriefcaseWindow() {
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    appPreferences.getBriefcaseDir()
        .map(FormCache::from)
        .ifPresent(FileSystemUtils::setFormCache);

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

    pushPanel = PushPanel.from(http, appPreferences, transferTerminationFuture);
    addPane(PushPanel.TAB_NAME, pushPanel.getContainer());

    exportPanel = ExportPanel.from(exportTerminationFuture, BriefcasePreferences.forClass(ExportPanel.class), appPreferences, BACKGROUND_EXECUTOR, analytics);
    addPane(ExportPanel.TAB_NAME, exportPanel.getForm().getContainer());

    settingsPanel = SettingsPanel.from(appPreferences, analytics).getContainer();
    addPane(SettingsPanel.TAB_NAME, settingsPanel);

    frame.addWindowListener(this);
    frame.setTitle(BRIEFCASE_VERSION);
    frame.setIconImage(imageIcon.getImage());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    if (isFirstLaunch(appPreferences)) {
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

  /**
   * Adds a pane to the JTabbedPane, and saves its index in a map from pane to index.
   */
  private void addPane(String title, Component pane) {
    paneToIndexMap.put(pane, tabbedPane.getTabCount());
    tabbedPane.addTab(title, null, pane, null);
  }

  @Override
  public void windowClosing(WindowEvent arg0) {
    exportTerminationFuture.markAsCancelled(new ExportAbortEvent("Main window closed"));
    transferTerminationFuture.markAsCancelled(new PullEvent.Abort("Main window closed"));
    transferTerminationFuture.markAsCancelled(new PushEvent.Abort("Main window closed"));
  }
}
