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

package org.opendatakit.briefcase.reused.http;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FakeHttp implements Http {
  private final Map<Query<?>, String> stubs = new ConcurrentHashMap<>();

  /**
   * Stub a pre-defined output for a given {@link Query}.
   */
  public void stub(Query query, String stub) {
    stubs.put(query, stub);
  }

  public <T> T execute(Query<T> query) {
    return Optional.ofNullable(stubs.get(query))
        .map(query::map)
        .orElseThrow(RuntimeException::new);
  }
}
