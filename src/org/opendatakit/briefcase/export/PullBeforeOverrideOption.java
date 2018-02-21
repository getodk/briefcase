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

import java.util.Optional;
import java.util.stream.Stream;

public enum PullBeforeOverrideOption {
  INHERIT("Use default", null), PULL("Pull before export", true), DONT_PULL("Do not pull before export", false);

  private final String label;
  private final Optional<Boolean> value;

  PullBeforeOverrideOption(String label, Boolean value) {
    this.label = label;
    this.value = Optional.ofNullable(value);
  }

  public static PullBeforeOverrideOption from(String name) {
    return Stream.of(values())
        .filter(value -> value.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown PullBeforeOverrideOption value " + name));
  }

  public static PullBeforeOverrideOption from(Optional<Boolean> maybeValue) {
    return maybeValue.map(value -> value ? PULL : DONT_PULL).orElse(INHERIT);
  }

  public Optional<Boolean> asBoolean() {
    return value;
  }

  @Override
  public String toString() {
    return label;
  }
}
