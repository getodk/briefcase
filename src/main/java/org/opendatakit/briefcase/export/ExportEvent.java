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

public class ExportEvent {
  private final String formId;
  private final String statusLine;
  private final boolean success;

  private ExportEvent(String formId, String statusLine, boolean success) {
    this.formId = formId;
    this.statusLine = statusLine;
    this.success = success;
  }

  public static ExportEvent progress(double percentage, String formId) {
    int base100Percentage = Double.valueOf(percentage * 100).intValue();
    String statusLine = String.format("Exported %s%% of the submissions", base100Percentage);
    return new ExportEvent(formId, statusLine, false);
  }

  public static ExportEvent start(String formId) {
    return new ExportEvent(formId, "Export has started", false);
  }

  public static ExportEvent end(long exported, String formId) {
    return new ExportEvent(formId, "Export has ended", false);
  }

  public static ExportEvent failure(String cause, String formId) {
    return new ExportEvent.Failure(formId, String.format("Failure: %s", cause), false);
  }

  public static ExportEvent failureSubmission(FormDefinition form, String instanceId, Throwable cause, String formId) {
    return new ExportEvent(
        formId,
        String.format("Can't export submission %s of form ID %s. Cause: %s", instanceId, formId, cause.getMessage()),
        false
    );
  }

  public static ExportEvent successForm(int total, String formId) {
    return new ExportEvent.Success(formId, String.format("Exported %d submission%s", total, sUnlessOne(total)), true);
  }

  public static ExportEvent partialSuccessForm(int exported, int total, String formId) {
    return new ExportEvent.Success(formId, String.format("Exported %d from %d submission%s", exported, total, sUnlessOne(total)), true);
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

  public static class Failure extends ExportEvent {
    private Failure(String formId, String statusLine, boolean success) {
      super(formId, statusLine, success);
    }
  }

  public static class Success extends ExportEvent {
    private Success(String formId, String statusLine, boolean success) {
      super(formId, statusLine, success);
    }
  }
}
