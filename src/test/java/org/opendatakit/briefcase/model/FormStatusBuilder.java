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
package org.opendatakit.briefcase.model;

import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.pull.aggregate.Cursor;

public class FormStatusBuilder {

  public static FormStatus buildFormStatus(int id) {
    try {
      return new FormStatus(new FormMetadata(
          FormKey.of("Form " + id, "form-" + id),
          Optional.ofNullable(Files.createTempFile("briefcase-form-" + id, ".xml")),
          Cursor.empty(),
          false,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      ));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static List<FormStatus> buildFormStatusList(int amount) {
    return IntStream.range(0, amount).boxed().map(FormStatusBuilder::buildFormStatus).collect(toList());
  }
}
