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

/**
 * This enum holds all the possible {@link Submission} validation statuses
 */
public enum ValidationStatus {
  /**
   * Initially all the {@link Submission} instances have this status. This is
   * OK when the form is not encrypted.
   */
  NOT_VALIDATED,
  /**
   * The incoming cryptographic signature has been matched against the parsed
   * values inside a submission and they don't match.
   */
  NOT_VALID,
  /**
   * The incoming cryptographic signature has been matched against the parsed
   * values inside a submission and they do match.
   */
  VALID;

  static ValidationStatus of(boolean value) {
    return value ? VALID : NOT_VALID;
  }

  /**
   * A {@link ValidationStatus} inside a CSV must be encoded as a boolean.
   *
   * @return a CSV compatible {@link String}
   */
  public String asCsvValue() {
    return this == VALID
        ? Boolean.TRUE.toString()
        : this == NOT_VALID
        ? Boolean.FALSE.toString()
        : null;
  }
}
