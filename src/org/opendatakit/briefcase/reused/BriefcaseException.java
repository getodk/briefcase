package org.opendatakit.briefcase.reused;

public class BriefcaseException extends RuntimeException {
  public final String message;

  public BriefcaseException(String message) {
    this.message = message;
  }
}
