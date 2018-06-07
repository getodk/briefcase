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
  private final boolean success;

  private ExportEvent(String formId, String statusLine, boolean success) {
    this.formId = formId;
    this.statusLine = statusLine;
    this.success = success;
  }

  public static ExportEvent progress(FormDefinition form, double percentage) {
    int base100Percentage = new Double(percentage * 100).intValue();
    String statusLine = String.format("Exported %s%% of the submissions", base100Percentage);
    return new ExportEvent(form.getFormId(), statusLine, false);
  }

  public static ExportEvent start(FormDefinition form) {
    return new ExportEvent(form.getFormId(), "Export has started", false);
  }

  public static ExportEvent end(FormDefinition form, long exported) {
    return new ExportEvent(form.getFormId(), "Export has ended", false);
  }

  public static ExportEvent failure(FormDefinition form, String cause) {
    return new ExportEvent(form.getFormId(), String.format("Failure: %s", cause), false);
  }

  public static ExportEvent failureSubmission(FormDefinition form, String instanceId, Throwable cause) {
    return new ExportEvent(
        form.getFormId(),
        String.format("Can't export submission %s of form ID %s. Cause: %s", instanceId, form.getFormId(), cause.getMessage()),
        false
    );
  }

  public static ExportEvent successForm(FormDefinition formDef, int total) {
    return new ExportEvent(formDef.getFormId(), String.format("Exported %d submission%s", total, sUnlessOne(total)), true);
  }

  public static ExportEvent partialSuccessForm(FormDefinition formDef, int exported, int total) {
    return new ExportEvent(formDef.getFormId(), String.format("Exported %d from %d submission%s", exported, total, sUnlessOne(total)), true);
  }

  public String getFormId() {
    return formId;
  }

  public String getStatusLine() {
    return statusLine;
  }

  public boolean isSuccess() {
    return success;
  }

  @SuppressWarnings("checkstyle:MethodName")
  private static String sUnlessOne(int num) {
    return num == 1 ? "" : "s";
  }
}
