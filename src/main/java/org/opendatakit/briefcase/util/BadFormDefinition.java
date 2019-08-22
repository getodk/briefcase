package org.opendatakit.briefcase.util;

public class BadFormDefinition extends Exception {
  public BadFormDefinition(String message) {
    super(message);
  }

  public BadFormDefinition(Throwable cause) {
    super(cause);
  }
}