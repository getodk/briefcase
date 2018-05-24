package org.opendatakit.briefcase.automation;

import java.nio.file.Path;
import java.util.Optional;

public class AutomationConfiguration {
  private Optional<Path> scriptLocation;
  private Optional<Path> exportDir;
  private Optional<String> username;
  private Optional<String> password;

  public AutomationConfiguration(Optional<Path> scriptLocation, Optional<Path> exportDir, Optional<String> username, Optional<String> password) {
    this.scriptLocation = scriptLocation;
    this.exportDir = exportDir;
    this.username = username;
    this.password = password;
  }

  public static AutomationConfiguration empty() {
    return new AutomationConfiguration(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public Optional<Path> getScriptLocation() {
    return scriptLocation;
  }

  public AutomationConfiguration setScriptLocation(Path path) {
    this.scriptLocation = Optional.ofNullable(path);
    return this;
  }

  public Optional<Path> getExportDir() {
    return exportDir;
  }

  public AutomationConfiguration setExportDir(Path path) {
    this.exportDir = Optional.ofNullable(path);
    return this;
  }

  public Optional<String> getUsername() {
    return username;
  }

  public AutomationConfiguration setUsername(String username) {
    this.username = Optional.ofNullable(username);
    return this;
  }

  public Optional<String> getPassword() {
    return password;
  }

  public AutomationConfiguration setPassword(String password) {
    this.password = Optional.ofNullable(password);
    return this;
  }
}
