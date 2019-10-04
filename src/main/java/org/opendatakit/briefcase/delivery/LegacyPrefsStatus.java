package org.opendatakit.briefcase.delivery;

public enum LegacyPrefsStatus {
  IMPORTED(true), IGNORED(true), UNDECIDED(false);

  private final boolean resolved;

  LegacyPrefsStatus(boolean resolved) {this.resolved = resolved;}

  public boolean isResolved() {
    return resolved;
  }

  public boolean isUnresolved() {
    return !resolved;
  }
}
