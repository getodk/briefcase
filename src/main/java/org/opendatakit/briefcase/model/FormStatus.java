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
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.model.form.FormMetadata;

public class FormStatus implements Serializable {

  private final FormMetadata formMetadata;
  private String statusString = "";
  private final StringBuilder statusHistory = new StringBuilder();

  public FormStatus(FormMetadata formMetadata) {
    this.formMetadata = formMetadata;
  }

  public synchronized void clearStatusHistory() {
    statusString = "";
    statusHistory.setLength(0);
  }

  public synchronized String getStatusHistory() {
    return statusHistory.toString();
  }

  public synchronized String getFormName() {
    return formMetadata.getKey().getName();
  }

  public boolean isEncrypted() {
    return formMetadata.isEncrypted();
  }

  public String getFormId() {
    return formMetadata.getKey().getId();
  }

  public Path getFormDir() {
    return formMetadata.getFormFile().getParent();
  }

  public Path getFormFile() {
    return formMetadata.getFormFile();
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

  public Path getSubmissionMediaFile(String instanceId, String filename) {
    return getSubmissionDir(instanceId).resolve(filename);
  }

  public Optional<String> getVersion() {
    return formMetadata.getKey().getVersion();
  }

  public FormMetadata getFormMetadata() {
    return formMetadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FormStatus that = (FormStatus) o;
    return Objects.equals(formMetadata, that.formMetadata) &&
        Objects.equals(statusString, that.statusString) &&
        Objects.equals(statusHistory, that.statusHistory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formMetadata, statusString, statusHistory);
  }
}
