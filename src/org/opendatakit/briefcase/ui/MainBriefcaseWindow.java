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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.model.BriefcaseAnalytics;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportAbortEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferAbortEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class MainBriefcaseWindow extends WindowAdapter implements UiStateChangeListener {
    private static final String BRIEFCASE_VERSION = "ODK Briefcase - " + BriefcasePreferences.VERSION;

    JFrame frame;
    private PushTransferPanel uploadPanel;
    private ExportPanel exportPanel;
    private SettingsPanel settingsPanel;
    private final TerminationFuture exportTerminationFuture = new TerminationFuture();
    private final TerminationFuture transferTerminationFuture = new TerminationFuture();
    private BriefcaseAnalytics briefcaseAnalytics = new BriefcaseAnalytics();
    final StorageLocation storageLocation;

    public static final String AGGREGATE_URL = "aggregate_url";
    public static final String DATE_FORMAT = "yyyy/MM/dd";
    public static final String EXCLUDE_MEDIA_EXPORT = "exclude_media_export";
    public static final String EXPORT_DIRECTORY = "export_directory";
    public static final String EXPORT_END_DATE = "export_end_date";
    public static final String EXPORT_FILENAME = "export_filename";
    public static final String EXPORT_START_DATE = "export_start_date";
    public static final String FORM_ID = "form_id";
    public static final String ODK_PASSWORD = "odk_password";
    public static final String ODK_USERNAME = "odk_username";
    public static final String OVERWRITE_CSV_EXPORT = "overwrite_csv_export";
    public static final String STORAGE_DIRECTORY = "storage_directory";
    public static final String ODK_DIR = "odk_directory";
    public static final String HELP = "help";
    public static final String VERSION = "version";
    public static final String PEM_FILE = "pem_file";

    private static final Log log = LogFactory.getLog(BaseFormParserForJavaRosa.class.getName());

    private final JTabbedPane tabbedPane;
    /** A map from each pane to its index in the JTabbedPane */
    private final Map<Component, Integer> paneToIndexMap = new HashMap<>();

    /**
     * Launch the application.
     */
    public static void main(String[] args) {

        if (args.length == 0) {

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        // Set System L&F
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                        MainBriefcaseWindow window = new MainBriefcaseWindow();
                        window.frame.setTitle(BRIEFCASE_VERSION);
                        ImageIcon icon = new ImageIcon(MainBriefcaseWindow.class.getClassLoader().getResource("odk_logo.png"));
                        window.frame.setIconImage(icon.getImage());
                        window.frame.setLocationRelativeTo(null);
                        window.frame.setVisible(true);
                    } catch (Exception e) {
                        log.error("failed to launch app", e);
                    }
                }
            });
        } else {
            Options options = addOptions();
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = null;

            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e1) {
                log.error("Launch Failed: " + e1.getMessage());
                showHelp(options);
                System.exit(1);
            }

            if (cmd.hasOption(HELP)) {
                showHelp(options);
                System.exit(1);
            }

            if (cmd.hasOption(VERSION)) {
                showVersion();
                System.exit(1);
            }

            // required for all operations
            if (!cmd.hasOption(FORM_ID) || !cmd.hasOption(STORAGE_DIRECTORY)) {
                log.error(FORM_ID + " and " + STORAGE_DIRECTORY + " are required");
                showHelp(options);
                System.exit(1);
            }

            // pull from collect or aggregate, not both
            if (cmd.hasOption(ODK_DIR) && cmd.hasOption(AGGREGATE_URL)) {
                log.error("Can only have one of " + ODK_DIR + " or " + AGGREGATE_URL);
                showHelp(options);
                System.exit(1);
            }

            // pull from aggregate
            if (cmd.hasOption(AGGREGATE_URL) && (!(cmd.hasOption(ODK_USERNAME) && cmd.hasOption(ODK_PASSWORD)))) {
                log.error(ODK_USERNAME + " and " + ODK_PASSWORD + " required when " + AGGREGATE_URL + " is specified");
                showHelp(options);
                System.exit(1);
            }

            // export files
            if (cmd.hasOption(EXPORT_DIRECTORY) && !cmd.hasOption(EXPORT_FILENAME) || !cmd.hasOption(EXPORT_DIRECTORY) && cmd.hasOption(EXPORT_FILENAME)) {
                log.error(EXPORT_DIRECTORY + " and " + EXPORT_FILENAME + " are both required to export");
                showHelp(options);
                System.exit(1);
            }

            if (cmd.hasOption(EXPORT_END_DATE)) {
                if (!testDateFormat(cmd.getOptionValue(EXPORT_END_DATE))) {
                    log.error("Invalid date format in " + EXPORT_END_DATE);
                    showHelp(options);
                    System.exit(1);
                }
            }
            if (cmd.hasOption(EXPORT_START_DATE)) {
                if (!testDateFormat(cmd.getOptionValue(EXPORT_START_DATE))) {
                    log.error("Invalid date format in " + EXPORT_START_DATE);
                    showHelp(options);
                    System.exit(1);
                }
            }


            BriefcaseCLI bcli = new BriefcaseCLI(cmd);
            bcli.run();
        }
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
        if (! enabled) {
            tabbedPane.setSelectedComponent(settingsPanel);
        }
        tabbedPane.setEnabled(enabled);
    }

    /**
     * Create the application.
     */
    private MainBriefcaseWindow() {
        briefcaseAnalytics.trackStartup();

        frame = new JFrame();
        frame.setBounds(100, 100, 680, 595);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        storageLocation = new StorageLocation();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.LEADING)
                                        .addComponent(tabbedPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 628,
                                                Short.MAX_VALUE))
                        .addContainerGap()));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
                        .addContainerGap()));

        PullTransferPanel gatherPanel = new PullTransferPanel(transferTerminationFuture);
        addPane(PullTransferPanel.TAB_NAME, gatherPanel);

        uploadPanel = new PushTransferPanel(transferTerminationFuture);
        addPane(PushTransferPanel.TAB_NAME, uploadPanel);

        exportPanel = new ExportPanel(exportTerminationFuture);
        addPane(ExportPanel.TAB_NAME, exportPanel);
        frame.getContentPane().setLayout(groupLayout);

        settingsPanel = new SettingsPanel(this);
        addPane(SettingsPanel.TAB_NAME, settingsPanel);

        frame.addWindowListener(this);

        storageLocation.establishBriefcaseStorageLocation(frame, this);
    }

    /** Adds a pane to the JTabbedPane, and saves its index in a map from pane to index. */
    private void addPane(String title, Component pane) {
        paneToIndexMap.put(pane, tabbedPane.getTabCount());
        tabbedPane.addTab(title, null, pane, null);
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        exportTerminationFuture.markAsCancelled(new ExportAbortEvent("Main window closed"));
        transferTerminationFuture.markAsCancelled(new TransferAbortEvent("Main window closed"));
    }

    static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar briefcase.jar", options);
    }

    static void showVersion() {
        System.out.println("version: " + BriefcasePreferences.VERSION);
    }

    /**
     * Setting up options for Command Line Interface
     * @return
     */
    static Options addOptions() {
        Options options = new Options();

        Option server = Option.builder("url")
                .argName("url")
                .hasArg()
                .longOpt(AGGREGATE_URL)
                .desc("ODK Aggregate URL (must start with http:// or https://)")
                .build();

        Option username = Option.builder("u")
                .argName("username")
                .hasArg()
                .longOpt(ODK_USERNAME)
                .desc("ODK username")
                .build();

        Option password = Option.builder("p")
                .argName("password")
                .hasArg()
                .longOpt(ODK_PASSWORD)
                .desc("ODK password")
                .build();

        Option formid = Option.builder("id")
                .argName("form_id")
                .hasArg()
                .longOpt(FORM_ID)
                .desc("Form ID of form to download and export")
                .build();

        Option storageDir = Option.builder("sd")
                .argName("/path/to/dir")
                .hasArg()
                .longOpt(STORAGE_DIRECTORY)
                .desc("Directory to create or find ODK Briefcase Storage directory (relative path unless it begins with / or C:\\)")
                .build();

        Option exportDir = Option.builder("ed")
                .argName("/path/to/dir")
                .hasArg()
                .longOpt(EXPORT_DIRECTORY)
                .desc("Directory to export the CSV and media files into (relative path unless it begins with / or C:\\)")
                .build();

        Option exportMedia = Option.builder("em")
                .longOpt(EXCLUDE_MEDIA_EXPORT)
                .desc("Flag to exclude media on export")
                .build();

        Option startDate = Option.builder("start")
                .argName(DATE_FORMAT)
                .hasArg()
                .longOpt(EXPORT_START_DATE)
                .desc("Include submission dates after (inclusive) this date in export to CSV")
                .build();

        Option endDate = Option.builder("end")
                .argName(DATE_FORMAT)
                .hasArg()
                .longOpt(EXPORT_END_DATE)
                .desc("Include submission dates before (exclusive) this date in export to CSV")
                .build();

        Option exportFilename = Option.builder("f")
                .argName("name.csv")
                .hasArg()
                .longOpt(EXPORT_FILENAME)
                .desc("File name for exported CSV")
                .build();

        Option overwrite = Option.builder("oc")
                .longOpt(OVERWRITE_CSV_EXPORT)
                .desc("Flag to overwrite CSV on export")
                .build();

        Option help = Option.builder("h")
                .longOpt(HELP)
                .desc("Print help information (this screen)")
                .build();

        Option version = Option.builder("v")
                .longOpt(VERSION)
                .desc("Print version information")
                .build();

        Option odkDir = Option.builder("od")
                .argName("/path/to/dir")
                .hasArg()
                .longOpt(ODK_DIR)
                .desc("/odk directory from ODK Collect (relative path unless it begins with / or C:\\)")
                .build();

        Option keyFile = Option.builder("pf")
                .argName("/path/to/file.pem")
                .hasArg()
                .longOpt(PEM_FILE)
                .desc("PEM private key file (relative path unless it begins with / or C:\\)")
                .build();

        options.addOption(server);
        options.addOption(username);
        options.addOption(password);
        options.addOption(formid);
        options.addOption(storageDir);
        options.addOption(exportDir);
        options.addOption(exportMedia);
        options.addOption(startDate);
        options.addOption(endDate);
        options.addOption(exportFilename);
        options.addOption(overwrite);
        options.addOption(help);
        options.addOption(version);
        options.addOption(odkDir);
        options.addOption(keyFile);

        return options;
    }

    static boolean testDateFormat(String date) {
        try {
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            df.parse(date);
        } catch (java.text.ParseException e) {
            return false;
        }
        return true;
    }
}
