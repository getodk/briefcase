package org.opendatakit.briefcase.reused.model.submission;

import java.util.Optional;

public class SubmissionKey {
  private final String formId;
  private final Optional<String> formVersion;
  private final String instanceId;

  public SubmissionKey(String formId, Optional<String> formVersion, String instanceId) {
    this.formId = formId;
    this.formVersion = formVersion;
    this.instanceId = instanceId;
  }

  public String getFormId() {
    return formId;
  }

  public Optional<String> getFormVersion() {
    return formVersion;
  }

  public String getInstanceId() {
    return instanceId;
  }
}
