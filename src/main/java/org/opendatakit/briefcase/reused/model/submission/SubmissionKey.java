package org.opendatakit.briefcase.reused.model.submission;

import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.model.XmlElement;

/**
 * This class holds the least amount of information required to
 * identify a form submission unambiguously:
 * - A form ID
 * - An optional form version
 * - An instance ID
 */
public class SubmissionKey {
  private final String formId;
  private final Optional<String> formVersion;
  private final String instanceId;

  public SubmissionKey(String formId, Optional<String> formVersion, String instanceId) {
    this.formId = formId;
    this.formVersion = formVersion;
    this.instanceId = instanceId;
  }

  static SubmissionKey from(XmlElement root) {
    return new SubmissionKey(
        extractFormId(root).orElseThrow(() -> new BriefcaseException("Unable to extract form id")),
        extractFormVersion(root),
        extractInstanceId(root).orElseThrow(() -> new BriefcaseException("Unable to extract instance id"))
    );
  }

  static SubmissionKey from(XmlElement root, String instanceId) {
    return new SubmissionKey(
        extractFormId(root).orElseThrow(() -> new BriefcaseException("Unable to extract form id")),
        extractFormVersion(root),
        instanceId
    );
  }

  private static Optional<String> extractFormId(XmlElement root) {
    return Optionals.race(
        root.getAttributeValue("id"),
        root.getAttributeValue("xmlns")
    );
  }

  private static Optional<String> extractFormVersion(XmlElement root) {
    return root.getAttributeValue("version");
  }

  public static Optional<String> extractInstanceId(XmlElement root) {
    return Optionals.race(
        root.findFirstElement("instanceID").flatMap(XmlElement::maybeValue),
        root.getAttributeValue("instanceID")
    );
  }

  public String getFormId() {
    return formId;
  }

  Optional<String> getFormVersion() {
    return formVersion;
  }

  public String getInstanceId() {
    return instanceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SubmissionKey that = (SubmissionKey) o;
    return Objects.equals(formId, that.formId) &&
        Objects.equals(formVersion, that.formVersion) &&
        Objects.equals(instanceId, that.instanceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formId, formVersion, instanceId);
  }

  @Override
  public String toString() {
    return "SubmissionKey{" +
        "formId='" + formId + '\'' +
        ", formVersion=" + formVersion +
        ", instanceId='" + instanceId + '\'' +
        '}';
  }

}
