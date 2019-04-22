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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.opendatakit.briefcase.reused.http.response.Response;

public class FakeHttp implements Http {
  private final Map<Request<?>, Response<String>> stubs = new ConcurrentHashMap<>();

  public void stub(Request<?> request, Response<String> stub) {
    stubs.put(request, stub);
  }

  public <T> Response<T> execute(Request<T> request) {
    return Optional.ofNullable(stubs.get(request))
        .orElseThrow(() -> new RuntimeException("No stub defined for Query " + request.toString()))
        .map(body -> request.map(new ByteArrayInputStream(Optional.ofNullable(body).orElse("").getBytes(UTF_8))));
  }

  @Override
  public Http reusingConnections() {
    return this;
  }
}
