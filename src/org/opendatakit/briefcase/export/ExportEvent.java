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
  private final String formName;
  private final String statusLine;
  private final boolean success;

  private ExportEvent(String formName, String statusLine, boolean success) {
    this.formName = formName;
    this.statusLine = statusLine;
    this.success = success;
  }

  public static ExportEvent progress(FormDefinition form, double percentage) {
    int base100Percentage = new Double(percentage * 100).intValue();
    String statusLine = String.format("Exported %s%% of the submissions", base100Percentage);
    return new ExportEvent(form.getFormName(), statusLine, false);
  }

  public static ExportEvent start(FormDefinition form) {
    return new ExportEvent(form.getFormName(), "Export has started", false);
  }

  public static ExportEvent end(FormDefinition form, long exported) {
    return new ExportEvent(form.getFormName(), "Export has ended", false);
  }

  public static ExportEvent failure(FormDefinition form, String cause) {
    return new ExportEvent.Failure(form.getFormName(), String.format("Failure: %s", cause), false);
  }

  public static ExportEvent failureSubmission(FormDefinition form, String instanceId, Throwable cause) {
    return new ExportEvent(
        form.getFormName(),
        String.format("Can't export submission %s of form %s. Cause: %s", instanceId, form.getFormName(), cause.getMessage()),
        false
    );
  }

  public static ExportEvent successForm(FormDefinition formDef, int total) {
    return new ExportEvent.Success(formDef.getFormName(), String.format("Exported %d submission%s", total, sUnlessOne(total)), true);
  }

  public static ExportEvent partialSuccessForm(FormDefinition formDef, int exported, int total) {
    return new ExportEvent.Success(formDef.getFormName(), String.format("Exported %d from %d submission%s", exported, total, sUnlessOne(total)), true);
  }

  public String getFormName() {
    return formName;
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
    private Failure(String formName, String statusLine, boolean success) {
      super(formName, statusLine, success);
    }
  }

  public static class Success extends ExportEvent {
    private Success(String formName, String statusLine, boolean success) {
      super(formName, statusLine, success);
    }
  }
}
