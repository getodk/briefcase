package org.opendatakit.briefcase.reused.cli;

import java.util.Optional;
import org.apache.commons.cli.Option;

public class Flag extends Param<Void> {
  Flag(String shortCode, Option option) {
    super(shortCode, option, Optional.empty());
  }

  public static org.opendatakit.briefcase.reused.cli.Flag of(String shortCode, String longCode, String description) {
    return new org.opendatakit.briefcase.reused.cli.Flag(shortCode, new Option(shortCode, longCode, false, description));
  }
}
