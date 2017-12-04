package org.opendatakit.briefcase;

import org.opendatakit.briefcase.ui.MainBriefcaseWindow;
import org.opendatakit.common.cli.Cli;

/**
 * Main launcher for Briefcase
 * <p>
 * It leverages the command-line {@link Cli} adapter to define operations and run
 * Briefcase with some command-line args
 */
public class Launcher {
  public static void main(String[] args) {
    new Cli()
        .otherwise(() -> MainBriefcaseWindow.main(args))
        .run(args);
  }
}
