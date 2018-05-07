/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.export;

public final class ExportEvent {
  private final String formId;
  private final String statusLine;

  private ExportEvent(String formId, String statusLine) {
    this.formId = formId;
    this.statusLine = statusLine;
  }

  public static ExportEvent progress(FormDefinition form, double percentage) {
    int base100Percentage = new Double(percentage * 100).intValue();
    String statusLine = String.format("Exported %s%% of the submissions", base100Percentage);
    return new ExportEvent(form.getFormId(), statusLine);
  }

  public static ExportEvent start(FormDefinition form) {
    return new ExportEvent(form.getFormId(), "Export has started");
  }

  public static ExportEvent end(FormDefinition form, long exported) {
    return new ExportEvent(form.getFormId(), "Export has ended");
  }

  public static ExportEvent failure(FormDefinition form, String cause) {
    return new ExportEvent(form.getFormId(), String.format("Failure: %s", cause));
  }

  public String getFormId() {
    return formId;
  }

  public String getStatusLine() {
    return statusLine;
  }
}
