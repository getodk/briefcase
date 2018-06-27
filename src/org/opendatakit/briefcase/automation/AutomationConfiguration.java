package org.opendatakit.briefcase.automation;

import java.nio.file.Path;
import java.util.Optional;

public class AutomationConfiguration {
  private Optional<Path> scriptLocation;

  public AutomationConfiguration(Optional<Path> scriptLocation) {
    this.scriptLocation = scriptLocation;
  }

  public static AutomationConfiguration empty() {
    return new AutomationConfiguration(Optional.empty());
  }

  public Optional<Path> getScriptLocation() {
    return scriptLocation;
  }

  public AutomationConfiguration setScriptLocation(Path path) {
    this.scriptLocation = Optional.ofNullable(path);
    return this;
  }
}
