/*
 * Copyright (C) 2014 University of Washington.
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

import static org.opendatakit.briefcase.operations.Export.export;
import static org.opendatakit.briefcase.operations.ImportFromODK.importODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.pullFormFromAggregate;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line interface contributed by Nafundi
 *
 * @author chartung@nafundi.com
 */
public class BriefcaseCLI {
  private static final String AGGREGATE_URL = "aggregate_url";
  private static final String DATE_FORMAT1 = "yyyy-MM-dd";
  private static final String DATE_FORMAT2 = "yyyy/MM/dd";
  private static final String EXCLUDE_MEDIA_EXPORT = "exclude_media_export";
  private static final String EXPORT_DIRECTORY = "export_directory";
  private static final String EXPORT_END_DATE = "export_end_date";
  private static final String EXPORT_FILENAME = "export_filename";
  private static final String EXPORT_START_DATE = "export_start_date";
  private static final String FORM_ID = "form_id";
  private static final String ODK_PASSWORD = "odk_password";
  private static final String ODK_USERNAME = "odk_username";
  private static final String OVERWRITE_CSV_EXPORT = "overwrite_csv_export";
  private static final String STORAGE_DIRECTORY = "storage_directory";
  private static final String ODK_DIR = "odk_directory";
  private static final String HELP = "help";
  private static final String VERSION = "version";
  private static final String PEM_FILE = "pem_file";
  private static final Logger log = LoggerFactory.getLogger(BriefcaseCLI.class);
  private final CommandLine cli;


  private BriefcaseCLI(CommandLine cli) {
    this.cli = cli;
  }

  static void launchLegacyCLI(String[] args) {
    Options options = addOptions();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      log.error("Failed to launch CLI", e);
      System.err.println("Failed to launch Briefcase CLI");
      showHelp(options);
      System.exit(1);
    }

    runLegacyCli(cmd, () -> showHelp(options));
  }

  public static void runLegacyCli(CommandLine cmd, Runnable helpShower) {
    System.err.println("" +
        "** WARNING ************************************************************\n" +
        "* You are using old commands which will soon be removed from the CLI. *\n" +
        "* Please run Briefcase with --help to learn about the new commands.   *\n" +
        "***********************************************************************\n");
    if (cmd.hasOption(HELP)) {
      helpShower.run();
      System.exit(1);
    }

    if (cmd.hasOption(VERSION)) {
      helpShower.run();
      System.exit(1);
    }

    // required for all operations
    if (!cmd.hasOption(FORM_ID) || !cmd.hasOption(STORAGE_DIRECTORY)) {
      System.err.println(FORM_ID + " and " + STORAGE_DIRECTORY + " are required");
      helpShower.run();
      System.exit(1);
    }

    // pull from collect or aggregate, not both
    if (cmd.hasOption(ODK_DIR) && cmd.hasOption(AGGREGATE_URL)) {
      System.err.println("Can only have one of " + ODK_DIR + " or " + AGGREGATE_URL);
      helpShower.run();
      System.exit(1);
    }

    // pull from aggregate
    if (cmd.hasOption(AGGREGATE_URL) && (!(cmd.hasOption(ODK_USERNAME) && cmd.hasOption(ODK_PASSWORD)))) {
      System.err.println(ODK_USERNAME + " and " + ODK_PASSWORD + " required when " + AGGREGATE_URL + " is specified");
      helpShower.run();
      System.exit(1);
    }

    // export files
    if (cmd.hasOption(EXPORT_DIRECTORY) && !cmd.hasOption(EXPORT_FILENAME) || !cmd.hasOption(EXPORT_DIRECTORY) && cmd.hasOption(EXPORT_FILENAME)) {
      System.err.println(EXPORT_DIRECTORY + " and " + EXPORT_FILENAME + " are both required to export");
      helpShower.run();
      System.exit(1);
    }

    if (cmd.hasOption(EXPORT_END_DATE))
      validateDateFormat(cmd, helpShower, EXPORT_END_DATE);

    if (cmd.hasOption(EXPORT_START_DATE))
      validateDateFormat(cmd, helpShower, EXPORT_START_DATE);


    BriefcaseCLI bcli = new BriefcaseCLI(cmd);
    bcli.run();
  }

  private static void validateDateFormat(CommandLine cmd, Runnable helpShower, String argName) {
    if (!testDateFormat(cmd.getOptionValue(argName))) {
      System.err.println("Invalid date format in " + argName);
      helpShower.run();
      System.exit(1);
    }
  }

  private static void showHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar briefcase.jar", options);
  }

  private static Options addOptions() {
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
        .argName(DATE_FORMAT1 + " or " + DATE_FORMAT2)
        .hasArg()
        .longOpt(EXPORT_START_DATE)
        .desc("Include submission dates after (inclusive) this date in export to CSV")
        .build();

    Option endDate = Option.builder("end")
        .argName(DATE_FORMAT1 + " or " + DATE_FORMAT2)
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

  private static boolean testDateFormat(String date) {
    date = date.replaceAll("/", "-");
    try {
      DateFormat df = new SimpleDateFormat(DATE_FORMAT1);
      df.parse(date);
    } catch (java.text.ParseException e) {
      return false;
    }
    return true;
  }

  public void run() {
    String username = cli.getOptionValue(ODK_USERNAME);
    String password = cli.getOptionValue(ODK_PASSWORD);
    String server = cli.getOptionValue(AGGREGATE_URL);
    String formid = cli.getOptionValue(FORM_ID);
    String storageDir = cli.getOptionValue(STORAGE_DIRECTORY);
    String fileName = cli.getOptionValue(EXPORT_FILENAME);
    String exportPath = cli.getOptionValue(EXPORT_DIRECTORY);
    String startDateString = cli.getOptionValue(EXPORT_START_DATE);
    String endDateString = cli.getOptionValue(EXPORT_END_DATE);
    // note that we invert incoming value
    boolean exportMedia = !cli.hasOption(EXCLUDE_MEDIA_EXPORT);
    boolean overwrite = cli.hasOption(OVERWRITE_CSV_EXPORT);
    String odkDir = cli.getOptionValue(ODK_DIR);
    String pemKeyFile = cli.getOptionValue(PEM_FILE);

    try {
      if (odkDir != null)
        importODK(storageDir, Paths.get(odkDir), Optional.empty());

      if (odkDir == null && server != null)
        pullFormFromAggregate(storageDir, formid, username, password, server, false, false);

      if (exportPath != null)
        export(
            storageDir,
            formid,
            Paths.get(exportPath),
            fileName,
            exportMedia,
            overwrite,
            false,
            Optional.ofNullable(startDateString).map(s -> LocalDate.parse(s.replaceAll("/", "-"))),
            Optional.ofNullable(endDateString).map(s -> LocalDate.parse(s.replaceAll("/", "-"))),
            Optional.ofNullable(pemKeyFile).map(Paths::get),
            false);
    } catch (BriefcaseException e) {
      System.err.println("Error: " + e.getMessage());
      log.error("Error", e);
      System.exit(1);
    } catch (Throwable t) {
      System.err.println("Unexpected error in Briefcase. Please review briefcase.log for more information. For help, post to https://forum.opendatakit.org/c/support");
      log.error("Error", t);
      System.exit(1);
    }
  }
}
