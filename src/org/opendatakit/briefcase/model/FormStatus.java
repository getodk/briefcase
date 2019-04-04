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

import java.nio.file.Path;
import java.util.Optional;

public class FormStatus {

  private static final int STATUS_HISTORY_MAX_BYTES = 1024 * 1024;

  private boolean isSelected = false;
  private IFormDefinition form;
  private String statusString = "";
  private final StringBuilder statusHistory = new StringBuilder();
  private boolean isSuccessful = true;

  public FormStatus(IFormDefinition form) {
    this.form = form;
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
    isSuccessful = true;
  }

  public synchronized void setStatusString(String statusString, boolean isSuccessful) {
    this.statusString = statusString;
    if (statusHistory.length() > STATUS_HISTORY_MAX_BYTES) {
      trimHistory(statusString.length());
    }
    statusHistory.append("\n");
    statusHistory.append(statusString);
    this.isSuccessful = this.isSuccessful && isSuccessful;
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

  public synchronized boolean isSuccessful() {
    return isSuccessful;
  }

  public synchronized String getFormName() {
    return form.getFormName();
  }

  public synchronized IFormDefinition getFormDefinition() {
    return form;
  }

  public boolean isEncrypted() {
    return ((BriefcaseFormDefinition) form).isFileEncryptedForm();
  }

  public String getFormId() {
    return form.getFormId();
  }

  public Optional<String> getManifestUrl() {
    if (!(form instanceof RemoteFormDefinition))
      return Optional.empty();
    return Optional.ofNullable(((RemoteFormDefinition) form).getManifestUrl());
  }

  public Path getFormDir(Path briefcaseDir) {
    return briefcaseDir.resolve("forms").resolve(stripIllegalChars(form.getFormName()));
  }

  public Path getFormMediaDir(Path briefcaseDir) {
    return getFormDir(briefcaseDir).resolve(stripIllegalChars(form.getFormName()) + "-media");
  }

  public Path getSubmissionDir(Path briefcaseDir, String instanceId) {
    return getFormDir(briefcaseDir).resolve("instances").resolve(stripIllegalChars(instanceId));
  }

}
