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

import java.util.stream.Stream;

/**
 * This enum encodes JavaRosa data types as described in {@link org.javarosa.core.model.Constants}
 */
enum DataType {
  UNSUPPORTED(-1),
  NULL(0),
  TEXT(1),
  INTEGER(2),
  DECIMAL(3),
  DATE(4),
  TIME(5),
  DATE_TIME(6),
  CHOICE(7),
  CHOICE_LIST(8),
  BOOLEAN(9),
  GEOPOINT(10),
  BARCODE(11),
  BINARY(12),
  LONG(13),
  GEOSHAPE(14),
  GEOTRACE(15);

  public final int javaRosaValue;

  DataType(int javaRosaValue) {
    this.javaRosaValue = javaRosaValue;
  }

  /**
   * Factory that will decode a JavaRosa data type constant into a {@link DataType}
   *
   * @param javaRosaValue an {@link Integer} with a JavaRosa code for a data type
   * @return the related {@link DataType} instance
   */
  static DataType from(int javaRosaValue) {
    return Stream.of(values())
        .filter(v -> v.javaRosaValue == javaRosaValue)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("DataType with value " + javaRosaValue + " not supported"));
  }
}
