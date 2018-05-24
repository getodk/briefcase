package org.opendatakit.briefcase.reused;

public class BriefcaseException extends RuntimeException {
  public BriefcaseException() {
    super();
  }

  public BriefcaseException(Throwable cause) {
    super(cause);
  }

  public BriefcaseException(String message) {
    super(message);
  }

  public BriefcaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
