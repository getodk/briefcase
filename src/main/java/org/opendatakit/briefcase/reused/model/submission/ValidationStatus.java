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
package org.opendatakit.briefcase.reused.model.submission;

public enum ValidationStatus {
  /**
   * Plain and encrypted submissions start having this validation status
   */
  NOT_VALIDATED(null),
  /**
   * The incoming cryptographic signature has been matched against the parsed
   * values inside a submission and they don't match.
   */
  NOT_VALID("false"),
  /**
   * The incoming cryptographic signature has been matched against the parsed
   * values inside a submission and they do match.
   */
  VALID("true");

  private final String csvValue;

  ValidationStatus(String csvValue) {
    this.csvValue = csvValue;
  }

  public String getCsvValue() {
    return csvValue;
  }
}
