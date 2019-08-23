/*
 * Copyright (C) 2011 University of Washington.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.model;

import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class FormStatus implements Serializable {
  private static final int STATUS_HISTORY_MAX_BYTES = 1024 * 1024;

  private final FormMetadata formMetadata;
  private boolean isSelected = false;
  private String statusString = "";
  private final StringBuilder statusHistory = new StringBuilder();
  private FormDefinition formDef;

  public FormStatus(IFormDefinition form) {
    FormMetadata formMetadata = FormMetadata.empty(FormKey.of(
        form.getFormName(),
        form.getFormId(),
        Optional.ofNullable(form.getVersionString())
    ));
    if (form instanceof OdkCollectFormDefinition) {
      OdkCollectFormDefinition collectFormDef = (OdkCollectFormDefinition) form;
      formMetadata = formMetadata.withFormFile(collectFormDef.getFormDefinitionFile().toPath());
    }
    this.formMetadata = formMetadata;
  }

  public FormStatus(FormMetadata formMetadata) {
    this.formMetadata = formMetadata;
  }

  public synchronized boolean isSelected() {
    return isSelected;
  }

  public synchronized void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public synchronized String getStatusString() {
    return statusString;
  }

  public synchronized void clearStatusHistory() {
    statusString = "";
    statusHistory.setLength(0);
  }

  public synchronized void setStatusString(String statusString) {
    this.statusString = statusString;
    if (statusHistory.length() > STATUS_HISTORY_MAX_BYTES) {
      trimHistory(statusString.length());
    }
    statusHistory.append("\n");
    statusHistory.append(statusString);
  }

  private void trimHistory(int len) {
    statusHistory.delete(0, len + 1);
    int lineEnd = statusHistory.indexOf("\n");
    if (lineEnd >= 0) {
      statusHistory.delete(0, lineEnd + 1);
    }
  }

  public synchronized String getStatusHistory() {
    return statusHistory.toString();
  }

  public synchronized String getFormName() {
    return formMetadata.getKey().getName();
  }

  public Optional<URL> getManifestUrl() {
    return formMetadata.getManifestUrl();
  }

  public Optional<URL> getDownloadUrl() {
    return formMetadata.getDownloadUrl();
  }

  public boolean isEncrypted() {
    return formMetadata.isEncrypted();
  }

  public String getFormId() {
    return formMetadata.getKey().getId();
  }

  public Path getFormDir() {
    return formMetadata.getFormFile().map(Path::getParent).orElseThrow(BriefcaseException::new);
  }

  public Path getFormFile() {
    return formMetadata.getFormFile().orElseThrow(BriefcaseException::new);
  }

  public Path getFormMediaDir() {
    return getFormDir().resolve(stripIllegalChars(formMetadata.getKey().getName()) + "-media");
  }

  public Path getFormMediaFile(String name) {
    return getFormMediaDir().resolve(name);
  }

  public Path getSubmissionsDir() {
    return getFormDir().resolve("instances");
  }

  public Path getSubmissionDir(String instanceId) {
    return getSubmissionsDir().resolve(instanceId.replace(":", ""));
  }

  public Path getSubmissionFile(String instanceId) {
    return getSubmissionDir(instanceId).resolve("submission.xml");
  }

  public Path getSubmissionMediaDir(String instanceId) {
    return getSubmissionDir(instanceId);
  }

  public Path getSubmissionMediaFile(String instanceId, String filename) {
    return getSubmissionDir(instanceId).resolve(filename);
  }

  public Optional<String> getVersion() {
    return formMetadata.getKey().getVersion();
  }

  public synchronized FormDefinition getFormDef() {
    if (formDef == null)
      formDef = FormDefinition.from(formMetadata.getFormFile().orElseThrow(BriefcaseException::new));
    return formDef;
  }

  public FormMetadata getFormMetadata() {
    return formMetadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormStatus that = (FormStatus) o;
    return isSelected == that.isSelected &&
        Objects.equals(formMetadata, that.formMetadata) &&
        Objects.equals(statusString, that.statusString) &&
        Objects.equals(statusHistory, that.statusHistory) &&
        Objects.equals(formDef, that.formDef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formMetadata, isSelected, statusString, statusHistory, formDef);
  }
}
