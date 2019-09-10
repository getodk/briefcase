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

package org.opendatakit.briefcase.delivery.ui;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.WEST;
import static java.lang.Runtime.getRuntime;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.GOOGLE_TRACKING_ID;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.infoMessage;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.makeClickable;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.uncheckedBrowse;
import static org.opendatakit.briefcase.reused.BriefcaseVersionManager.getLatestUrl;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchAsync;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.delivery.ui.export.ExportPanel;
import org.opendatakit.briefcase.delivery.ui.reused.Analytics;
import org.opendatakit.briefcase.delivery.ui.settings.SettingsPanel;
import org.opendatakit.briefcase.delivery.ui.transfer.pull.PullPanel;
import org.opendatakit.briefcase.delivery.ui.transfer.push.PushPanel;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.model.Host;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBriefcaseWindow extends WindowAdapter {
  private static final String TRACKING_WARNING = "" +
      "We now send usage data (e.g., operating system, version number) and crash logs\n" +
      "to the core developers to help prioritize features and fixes.\n\n" +
      "If you do not want to contribute your usage data or crash logs, please uncheck\n" +
      "that setting in the Settings tab.\n";
  private static final String BRIEFCASE_WELCOME = "" +
      "Welcome to ODK Briefcase! Here are three things you should know to get started.\n" +
      "\n" +
      "1. You must set a Storage Location where Briefcase will store data that it needs\n" +
      "    to operate. You will not be able to use Briefcase until you set this location.\n\n" +
      "2. We send usage data (e.g., operating system, version number) and crash logs\n" +
      "    to the core developers to help prioritize features and fixes. If you do not\n" +
      "    want to contribute your usage data or crash logs, please uncheck that setting.\n\n" +
      "3. ODK is a community-powered project and the community lives at\n" +
      "    https://forum.opendatakit.org. Stop by for a visit and introduce yourself!\n" +
      "\n";
  private static final Logger log = LoggerFactory.getLogger(MainBriefcaseWindow.class.getName());
  public static final String APP_NAME = "ODK Briefcase";

  private final JFrame frame = new JFrame();
  private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
  private final JLabel versionLabel = new JLabel("Checking for updates...");

  public static void launchGUI(Container container) {
    try {
      if (Host.isLinux())
        UIManager.setLookAndFeel(new MetalLookAndFeel());
      else
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      new MainBriefcaseWindow(container);
    } catch (Exception e) {
      log.error("Failed to launch GUI", e);
      System.err.println("Failed to launch Briefcase GUI");
      System.exit(1);
    }
  }

  private MainBriefcaseWindow(Container container) {
    // Create all dependencies
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    BriefcasePreferences pullPreferences = BriefcasePreferences.forClass(PullPanel.class);
    BriefcasePreferences exportPreferences = BriefcasePreferences.forClass(ExportPanel.class);

    Analytics analytics = Analytics.from(
        GOOGLE_TRACKING_ID,
        VERSION,
        BriefcasePreferences.getUniqueUserID(),
        Toolkit.getDefaultToolkit().getScreenSize(),
        frame::getSize
    );
    analytics.enableTracking(BriefcasePreferences.getBriefcaseTrackingConsentProperty());
    analytics.enter("Briefcase");
    getRuntime().addShutdownHook(new Thread(() -> analytics.leave("Briefcase")));

    // Add panes to the tabbedPane
    addPane(PullPanel.TAB_NAME, PullPanel.from(container, appPreferences, pullPreferences, analytics).getContainer());
    addPane(PushPanel.TAB_NAME, PushPanel.from(container, analytics, appPreferences).getContainer());
    addPane(ExportPanel.TAB_NAME, ExportPanel.from(container, exportPreferences, appPreferences, pullPreferences, analytics).getForm().getContainer());
    addPane(SettingsPanel.TAB_NAME, SettingsPanel.from(container, analytics).getContainer());

    // Set up the frame and put the UI components in it
    frame.addWindowListener(this);
    frame.setLayout(new GridBagLayout());
    frame.setTitle(APP_NAME);
    frame.setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("odk_logo.png"))).getImage());
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
    GridBagConstraints versionLabelConstraints;
    versionLabelConstraints = new GridBagConstraints();
    versionLabelConstraints.gridx = 0;
    versionLabelConstraints.gridy = 1;
    versionLabelConstraints.anchor = CENTER;
    versionLabelConstraints.insets = new Insets(5, 10, 5, 10);
    frame.add(versionLabel, versionLabelConstraints);
    frame.pack();

    launchAsync(run(rs -> {
      if (container.versionManager.isUpToDate()) {
        this.removeVersionLabel();
      } else {
        versionLabel.setText("Update available");
        makeClickable(versionLabel, () -> uncheckedBrowse(getLatestUrl()));
      }
    }));

    AnnotationProcessor.process(this);

    if (isFirstLaunch()) {
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

  private void removeVersionLabel() {
    versionLabel.setVisible(false);
    frame.remove(versionLabel);
    frame.pack();
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

  private boolean isFirstLaunch() {
    // TODO Implement this using the db. For now, skip
    return false;
  }

  private void addPane(String title, Component pane) {
    tabbedPane.addTab(title, null, pane, null);
  }

}
