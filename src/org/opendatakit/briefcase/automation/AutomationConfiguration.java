package org.opendatakit.briefcase.automation;

import java.nio.file.Path;
import java.util.Optional;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class AutomationConfiguration {
  private Optional<Path> scriptLocation;
  private Optional<ExportConfiguration> exportConfiguration;

  public AutomationConfiguration(Optional<Path> scriptLocation, Optional<ExportConfiguration> exportConfiguration) {
    this.scriptLocation = scriptLocation;
    this.exportConfiguration = exportConfiguration;
  }

  public static AutomationConfiguration empty() {
    return new AutomationConfiguration(Optional.empty(), Optional.empty());
  }

  public Optional<Path> getScriptLocation() {
    return scriptLocation;
  }

  public Optional<ExportConfiguration> getExportConfiguration() {
    return exportConfiguration;
  }
}
