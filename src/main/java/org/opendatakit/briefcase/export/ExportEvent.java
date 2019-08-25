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

import org.opendatakit.briefcase.model.form.FormKey;

public class ExportEvent {
  private final FormKey formKey;
  private final String formId;
  private final String statusLine;
  private final boolean success;

  private ExportEvent(FormKey formKey, String statusLine, boolean success) {
    this.formKey = formKey;
    this.formId = formKey.getId();
    this.statusLine = statusLine;
    this.success = success;
  }

  static ExportEvent progress(double percentage, FormKey formKey) {
    int base100Percentage = Double.valueOf(percentage * 100).intValue();
    String statusLine = String.format("Exported %s%% of the submissions", base100Percentage);
    return new ExportEvent(formKey, statusLine, false);
  }

  public static ExportEvent start(FormKey formKey) {
    return new ExportEvent(formKey, "Export has started", false);
  }

  public static ExportEvent end(FormKey formKey) {
    return new ExportEvent(formKey, "Export has ended", false);
  }

  static ExportEvent failure(String cause, FormKey formKey) {
    return new ExportEvent.Failure(String.format("Failure: %s", cause), false, formKey);
  }

  static ExportEvent failureSubmission(String instanceId, Throwable cause, FormKey formKey) {
    return new ExportEvent(
        formKey,
        String.format("Can't export submission %s of form ID %s. Cause: %s", instanceId, formKey.getId(), cause.getMessage()),
        false
    );
  }

  public static ExportEvent successForm(int total, FormKey formKey) {
    return new ExportEvent.Success(String.format("Exported %d submission%s", total, sUnlessOne(total)), true, formKey);
  }

  static ExportEvent partialSuccessForm(int exported, int total, FormKey formKey) {
    return new ExportEvent.Success(String.format("Exported %d from %d submission%s", exported, total, sUnlessOne(total)), true, formKey);
  }

  public String getFormId() {
    return formId;
  }

  public String getMessage() {
    return statusLine;
  }

  public boolean isSuccess() {
    return success;
  }

  @SuppressWarnings("checkstyle:MethodName")
  private static String sUnlessOne(int num) {
    return num == 1 ? "" : "s";
  }

  public FormKey getFormKey() {
    return formKey;
  }

  public static class Failure extends ExportEvent {
    private Failure(String statusLine, boolean success, FormKey formKey) {
      super(formKey, statusLine, success);
    }
  }

  public static class Success extends ExportEvent {
    private Success(String statusLine, boolean success, FormKey formKey) {
      super(formKey, statusLine, success);
    }
  }
}
