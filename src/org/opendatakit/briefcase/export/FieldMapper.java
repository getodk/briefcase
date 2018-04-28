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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.opendatakit.briefcase.util.Pair;

/**
 * This Functional Interface represents the operation of transformation of some
 * submission field's value to a CSV list of key-value pairs
 */
@FunctionalInterface
interface FieldMapper {
  List<Pair<String, String>> apply(String localId, Path workingDir, Model model, Optional<XmlElement> maybeElement, boolean exportMedia, Path exportMediaPath);

  /**
   * {@link FieldMapper} factory that will just apply some concrete {@link XmlElement} mapper
   * assuming that there is just one key-value pair to be returned.
   *
   * @param mapper the {@link Function} that must transform a {@link XmlElement} into
   *               the CSV key-value pair
   * @return a {@link FieldMapper} instance
   * @see FieldMapper#simpleMapper(Function, int)
   */
  static FieldMapper simpleMapper(Function<XmlElement, List<Pair<String, String>>> mapper) {
    return simpleMapper(mapper, 1);
  }

  /**
   * {@link FieldMapper} factory that will just apply some concrete {@link XmlElement} mapper
   * assuming that there is a fixed amount of key-value pairs to be returned.
   * <p>
   * If the {@link XmlElement} that the resulting {@link FieldMapper} gets is not present,
   * it will produce a {@link List} of empty key-value pairs of the correct size.
   *
   * @param mapper     the {@link Function} that must transform a {@link XmlElement} into
   *                   the CSV key-value pair
   * @param outputSize the {@link Integer} amount of key-value pairs that will be produced
   * @return a {@link FieldMapper} instance
   */
  static FieldMapper simpleMapper(Function<XmlElement, List<Pair<String, String>>> mapper, int outputSize) {
    return (localId, workingDir, model, element, exportMedia, exportMediaPath) -> element
        .map(mapper)
        .orElse(CsvMapper.empty(model.fqn(), outputSize));
  }

}
