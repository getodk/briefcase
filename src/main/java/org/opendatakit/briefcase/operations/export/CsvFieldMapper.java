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

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormModel;

/**
 * This Functional Interface represents the operation of transformation of a
 * submission field's value to a stream of CSV key-value pairs.
 * <p>
 * The apply method returns a list of column name and value pairs because we
 * need to support a weird empty/null value encoding scheme described <a href="https://github.com/opendatakit/briefcase/blob/master/docs/export-format.md#non-empty-value-codification">in the docs</a>.
 * <p>
 * Normally, the apply method should return just a stream of string values.
 */
@FunctionalInterface
interface CsvFieldMapper {
  // TODO Normalize the weird empty/null value encoding scheme and simplify this method to return a Stream<String> of just csv column values
  // TODO Simplify args by passing the ExportConfiguration object
  Stream<Pair<String, String>> apply(String formName, String localId, Path workingDir, FormModel model, Optional<XmlElement> maybeElement, ExportConfiguration configuration);

  default CsvFieldMapper andThen(CsvFieldMapper other) {
    return compose(this, other);
  }

  default CsvFieldMapper map(Function<Stream<Pair<String, String>>, Stream<Pair<String, String>>> mapper) {
    return decorateOutput(this, mapper);
  }

  static CsvFieldMapper compose(CsvFieldMapper a, CsvFieldMapper b) {
    return (formName, localId, workingDir, model, maybeElement, configuration) -> Stream.concat(
        a.apply(formName, localId, workingDir, model, maybeElement, configuration),
        b.apply(formName, localId, workingDir, model, maybeElement, configuration)
    );
  }

  static CsvFieldMapper decorateOutput(CsvFieldMapper mapper, Function<Stream<Pair<String, String>>, Stream<Pair<String, String>>> decorator) {
    return (formName, localId, workingDir, model, maybeElement, configuration) ->
        decorator.apply(mapper.apply(formName, localId, workingDir, model, maybeElement, configuration));
  }
}
