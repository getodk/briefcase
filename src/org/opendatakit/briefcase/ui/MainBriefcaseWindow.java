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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.model.BriefcaseAnalytics;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportAbortEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.pubsub.PubSub;
import org.opendatakit.common.pubsub.ThreadSafePubSub;

public class MainBriefcaseWindow extends WindowAdapter implements UiStateChangeListener {
  private static final String APP_NAME = "ODK Briefcase";
  private static final String BRIEFCASE_VERSION = APP_NAME + " - " + BriefcasePreferences.VERSION;
  private final ImageIcon imageIcon = new ImageIcon(getClass().getClassLoader().getResource("odk_logo.png"));

  JFrame frame;
  private PushTransferPanel uploadPanel;
  private ExportPanel exportPanel;
  private SettingsPanel settingsPanel;
  private final TerminationFuture exportTerminationFuture = new TerminationFuture();
  private final TerminationFuture transferTerminationFuture = new TerminationFuture();
  final BriefcaseAnalytics briefcaseAnalytics = new BriefcaseAnalytics();
  final StorageLocation storageLocation;

  private static final Log log = LogFactory.getLog(BaseFormParserForJavaRosa.class.getName());

  private final JTabbedPane tabbedPane;
  /**
   * A map from each pane to its index in the JTabbedPane
   */
  private final Map<Component, Integer> paneToIndexMap = new HashMap<>();

  public static void launchGUI() {
    PubSub pubSub = new ThreadSafePubSub();
    Executor executor = ForkJoinPool.commonPool();
    ExportAction exportAction = new ExportAction(pubSub, executor);

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          MainBriefcaseWindow window = new MainBriefcaseWindow(pubSub, exportAction);
        } catch (Exception e) {
          log.error("failed to launch app", e);
        }
      }
    });
  }

  @Override
  public void setFullUIEnabled(boolean enabled) {
    final String briefcaseDirectory = BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull();
    settingsPanel.getTxtBriefcaseDir().setText(briefcaseDirectory == null ?
        "" : briefcaseDirectory + File.separator + StorageLocation.BRIEFCASE_DIR);

    if (enabled) {
      exportPanel.updateComboBox();
      uploadPanel.updateFormStatuses();
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
   * @param pubSub
   * @param exportAction
   */
  private MainBriefcaseWindow(PubSub pubSub, ExportAction exportAction) {
    briefcaseAnalytics.trackStartup();

    frame = new JFrame();
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    storageLocation = new StorageLocation();
    createFormCache();

    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    frame.setContentPane(tabbedPane);

    PullTransferPanel gatherPanel = new PullTransferPanel(transferTerminationFuture);
    addPane(PullTransferPanel.TAB_NAME, gatherPanel);

    uploadPanel = new PushTransferPanel(transferTerminationFuture);
    addPane(PushTransferPanel.TAB_NAME, uploadPanel);

    exportPanel = new ExportPanel(pubSub, exportAction, exportTerminationFuture);
    addPane(ExportPanel.TAB_NAME, exportPanel);

    settingsPanel = new SettingsPanel(this);
    addPane(SettingsPanel.TAB_NAME, settingsPanel);

    frame.addWindowListener(this);
    frame.setTitle(BRIEFCASE_VERSION);
    frame.setIconImage(imageIcon.getImage());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    storageLocation.establishBriefcaseStorageLocation(frame, this);

    showIntroDialogIfNeeded();
  }

  private void createFormCache() {
    if (BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull() != null) {
      FileSystemUtils.createFormCacheInBriefcaseFolder();
    }
  }

  private void showIntroDialogIfNeeded() {
    if (BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull() == null) {
      JOptionPane.showMessageDialog(frame, MessageStrings.BRIEFCASE_WELCOME, APP_NAME,
          JOptionPane.INFORMATION_MESSAGE, imageIcon);
    }
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
    transferTerminationFuture.markAsCancelled(new TransferAbortEvent("Main window closed"));
  }
}
