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
package org.opendatakit.briefcase.operations;

import org.opendatakit.briefcase.model.IFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class ExportException extends BriefcaseException {

  public ExportException(IFormDefinition formDefinition) {
    super("Failure exporting form with formId " + formDefinition.getFormId());
  }

  public ExportException(IFormDefinition formDefinition, String cause) {
    super("Failure exporting form with formId " + formDefinition.getFormId() + ". Cause: " + cause);
  }
}
