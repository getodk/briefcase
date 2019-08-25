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

package org.opendatakit.briefcase.operations.export;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.model.XmlElement;

class SplitSelectMultiples {
  static CsvFieldMapper decorate(CsvFieldMapper mapper) {
    return (formName, localId, workingDir, model, element, configuration) -> {
      List<Pair<String, String>> output = new ArrayList<>();
      output.addAll(mapper.apply(formName, localId, workingDir, model, element, configuration).collect(toList()));
      if (model.isChoiceList()) {
        List<String> values = Arrays.stream(element
            .flatMap(XmlElement::maybeValue)
            .orElse("").split("\\s+"))
            .collect(toList());
        List<Pair<String, String>> extraPairs = model.getChoices().stream()
            .map(choice -> Pair.of(
                model.getName() + "/" + choice.getValue(),
                values.contains(choice.getValue()) ? "1" : "0"
            ))
            .collect(toList());
        output.addAll(extraPairs);
      }
      return output.stream();
    };
  }
}
